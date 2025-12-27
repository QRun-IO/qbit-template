# Extension Patterns

QBits can extend QQQ functionality by implementing existing extension point interfaces. This document covers the available hooks and how to use them.

## Extension Points Overview

| Extension Point | Interface | Registration | Use Case |
|-----------------|-----------|--------------|----------|
| Audit customization | `AuditActionCustomizerInterface` | `SupplementalCustomizer` | Modify audit records, mask sensitive data |
| Custom permissions | `CustomPermissionChecker` | `QPermissionRules` | AWS IAM-style policies, custom auth logic |
| Table CRUD hooks | `TableCustomizerInterface` | Per-table metadata | Validate, transform, trigger actions |
| Instance enrichment | `QInstanceEnricherPluginInterface` | Plugin discovery | Add computed metadata |
| Instance validation | `QInstanceValidatorPluginInterface` | Plugin discovery | Custom validation rules |
| Field metadata | `QSupplementalFieldMetaData` | Per-field | PHI flags, custom field properties |
| Table metadata | `QSupplementalTableMetaData` | Per-table | Custom table properties |

## Example: PHI Field Masking

This example shows how to build a HIPAA/PHI QBit that masks sensitive fields in audit logs.

### 1. Define Supplemental Field Metadata

```java
public class PhiFieldMetaData extends QSupplementalFieldMetaData
{
   public static final String TYPE = "phi";

   private Boolean maskInAudit = true;
   private Boolean maskInLogs = true;
   private String maskValue = "[PHI REDACTED]";

   @Override
   public String getType()
   {
      return TYPE;
   }

   // Getters, setters, fluent setters...
}
```

### 2. Implement Audit Customizer

```java
public class PhiAuditMasker implements AuditActionCustomizerInterface
{
   @Override
   public void customizeInput(AuditSingleInput input)
   {
      QTableMetaData table = QContext.getQInstance().getTable(input.getTableName());
      if(table == null)
      {
         return;
      }

      for(QRecord detail : input.getDetails())
      {
         String fieldName = detail.getValueString("fieldName");
         QFieldMetaData field = table.getField(fieldName);
         if(field == null)
         {
            continue;
         }

         PhiFieldMetaData phi = (PhiFieldMetaData) field.getSupplementalMetaData(PhiFieldMetaData.TYPE);
         if(phi != null && Boolean.TRUE.equals(phi.getMaskInAudit()))
         {
            detail.setValue("oldValue", phi.getMaskValue());
            detail.setValue("newValue", phi.getMaskValue());
         }
      }
   }
}
```

### 3. Register in Producer

```java
@Override
public void produce(QInstance qInstance, String namespace) throws QException
{
   // ... create QBitMetaData ...

   // Register the audit customizer
   qInstance.addSupplementalCustomizer(
      AuditActionCustomizerInterface.CUSTOMIZER_TYPE,
      new QCodeReference(PhiAuditMasker.class));

   // ... finish producing ...
}
```

### 4. Application Usage

Applications mark fields as PHI when defining their entities:

```java
new QFieldMetaData("ssn", QFieldType.STRING)
   .withLabel("Social Security Number")
   .withSupplementalMetaData(new PhiFieldMetaData()
      .withMaskInAudit(true)
      .withMaskInLogs(true));
```

## Example: Custom Permission Checker

Implement AWS IAM-style policy evaluation:

```java
public class PolicyPermissionChecker extends CustomPermissionChecker
{
   @Override
   public void checkPermissionsThrowing(AbstractActionInput input,
                                        MetaDataWithPermissionRules metaData)
      throws QPermissionDeniedException
   {
      QSession session = QContext.getQSession();

      // Get policies attached to user
      List<Policy> policies = loadUserPolicies(session.getUser().getIdReference());

      // Evaluate policies against requested action
      String action = deriveAction(input, metaData);
      String resource = deriveResource(input, metaData);

      PolicyEvaluationResult result = evaluatePolicies(policies, action, resource);

      if(result.isDenied())
      {
         throw new QPermissionDeniedException("Access denied by policy: " + result.getReason());
      }
   }
}
```

Register on tables or globally:

```java
// Per-table
table.withPermissionRules(new QPermissionRules()
   .withCustomPermissionChecker(new QCodeReference(PolicyPermissionChecker.class)));

// Or register in QBit producer for all tables
```

## Example: Table CRUD Hooks

Add validation or side effects to table operations:

```java
public class OrderTableCustomizer implements TableCustomizerInterface
{
   @Override
   public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
   {
      for(QRecord record : records)
      {
         // Auto-generate order number
         if(record.getValue("orderNumber") == null)
         {
            record.setValue("orderNumber", generateOrderNumber());
         }

         // Validate total
         BigDecimal total = record.getValueBigDecimal("total");
         if(total != null && total.compareTo(BigDecimal.ZERO) < 0)
         {
            record.addError(new BadInputStatusMessage("Total cannot be negative"));
         }
      }
      return records;
   }

   @Override
   public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
   {
      // Send notification for new orders
      for(QRecord record : records)
      {
         sendOrderConfirmation(record);
      }
      return records;
   }
}
```

## Extension Point Reference

### AuditActionCustomizerInterface

- `customizeInput(AuditSingleInput)` - Modify before audit record created
- `customizeRecord(QRecord, AuditSingleInput)` - Modify audit record before insert

### CustomPermissionChecker

- `checkPermissionsThrowing(AbstractActionInput, MetaDataWithPermissionRules)` - Throw if denied
- `handlesBuildAvailablePermission()` - Return true if you provide custom permissions
- `buildAvailablePermission()` - Return custom AvailablePermission objects

### TableCustomizerInterface

- `preInsert/postInsert` - Before/after insert
- `preUpdate/postUpdate` - Before/after update (with old record access)
- `preDelete/postDelete` - Before/after delete
- `postQuery` - After query results returned

### QSupplementalFieldMetaData

- `getType()` - Unique identifier for your metadata type
- `includeInFrontendMetaData()` - Whether to send to frontend
- `enrich(QInstance, QFieldMetaData)` - Enrich during instance setup
- `validate(QInstance, QFieldMetaData, QInstanceValidator)` - Validate field config
