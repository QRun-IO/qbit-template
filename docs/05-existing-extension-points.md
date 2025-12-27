# QQQ Extension Points Reference

This document provides a complete reference of QQQ's extension point interfaces that QBits can implement.

## Audit System

### AuditActionCustomizerInterface

**Location:** `com.kingsrook.qqq.backend.core.actions.audits`

**Registration:** Via `QInstance.addSupplementalCustomizer()`

```java
qInstance.addSupplementalCustomizer(
   AuditActionCustomizerInterface.CUSTOMIZER_TYPE,
   new QCodeReference(MyAuditCustomizer.class));
```

**Methods:**

| Method | Description |
|--------|-------------|
| `customizeInput(AuditSingleInput)` | Modify audit input before record creation |
| `customizeRecord(QRecord, AuditSingleInput)` | Modify audit record before insert |

**Use Cases:**
- Mask sensitive field values (PHI, PII)
- Add additional audit context
- Filter which changes are audited
- Route audits to external systems

## Security & Permissions

### CustomPermissionChecker

**Location:** `com.kingsrook.qqq.backend.core.actions.permissions`

**Registration:** Via `QPermissionRules.withCustomPermissionChecker()`

```java
table.withPermissionRules(new QPermissionRules()
   .withCustomPermissionChecker(new QCodeReference(MyChecker.class)));
```

**Methods:**

| Method | Description |
|--------|-------------|
| `checkPermissionsThrowing(AbstractActionInput, MetaDataWithPermissionRules)` | Throw if denied |
| `handlesBuildAvailablePermission()` | Return true if providing custom permissions |
| `buildAvailablePermission()` | Return AvailablePermission objects |

**Use Cases:**
- AWS IAM-style policy evaluation
- Attribute-based access control (ABAC)
- Dynamic permissions based on record data
- Integration with external auth systems

## Table Operations

### TableCustomizerInterface

**Location:** `com.kingsrook.qqq.backend.core.actions.customizers`

**Registration:** Per-table via metadata

```java
table.withCustomizer(TableCustomizerInterface.class,
   new QCodeReference(MyTableCustomizer.class));
```

**Methods:**

| Method | Description |
|--------|-------------|
| `postQuery(QueryOrGetInputInterface, List<QRecord>)` | After query results |
| `preInsert(InsertInput, List<QRecord>, boolean)` | Before insert |
| `postInsert(InsertInput, List<QRecord>)` | After insert |
| `preUpdate(UpdateInput, List<QRecord>, boolean, Optional<List<QRecord>>)` | Before update |
| `postUpdate(UpdateInput, List<QRecord>, Optional<List<QRecord>>)` | After update |
| `preDelete(DeleteInput, List<QRecord>, boolean)` | Before delete |
| `postDelete(DeleteInput, List<QRecord>)` | After delete |
| `whenToRunPreInsert()` | Control timing (default: AFTER_ALL_VALIDATIONS) |

**Use Cases:**
- Field validation beyond basic rules
- Auto-populate fields
- Trigger side effects (notifications, logging)
- Cascade operations to related records

## Instance Enrichment & Validation

### QInstanceEnricherPluginInterface

**Location:** `com.kingsrook.qqq.backend.core.instances`

**Registration:** Discovered automatically via classpath scanning

**Methods:**

| Method | Description |
|--------|-------------|
| `enrich(T object, QInstance)` | Add/modify metadata |
| `isEnabled()` | Whether plugin is active |
| `getPluginIdentifier()` | Unique identifier |

**Use Cases:**
- Add computed metadata based on configuration
- Inject default values into tables/fields
- Cross-reference validation

### QInstanceValidatorPluginInterface

**Location:** `com.kingsrook.qqq.backend.core.instances`

**Registration:** Discovered automatically via classpath scanning

**Methods:**

| Method | Description |
|--------|-------------|
| `validate(T object, QInstance, QInstanceValidator)` | Validate metadata |
| `isEnabled()` | Whether plugin is active |
| `getPluginIdentifier()` | Unique identifier |

**Use Cases:**
- Custom validation rules for your QBit's metadata
- Cross-QBit dependency validation
- Configuration consistency checks

## Supplemental Metadata

### QSupplementalFieldMetaData

**Location:** `com.kingsrook.qqq.backend.core.model.metadata.fields`

**Registration:** Per-field via `withSupplementalMetaData()`

```java
field.withSupplementalMetaData(new MyFieldMetaData().withOption(true));
```

**Abstract Methods:**

| Method | Description |
|--------|-------------|
| `getType()` | Unique identifier for this metadata type |

**Optional Override Methods:**

| Method | Description |
|--------|-------------|
| `includeInFrontendMetaData()` | Whether to send to frontend (default: false) |
| `enrich(QInstance, QFieldMetaData)` | Enrich during instance setup |
| `validate(QInstance, QFieldMetaData, QInstanceValidator)` | Validate configuration |

**Use Cases:**
- PHI/PII field marking
- Custom display options
- Field-level feature flags
- Integration metadata

### QSupplementalTableMetaData

**Location:** `com.kingsrook.qqq.backend.core.model.metadata.tables`

Similar pattern to QSupplementalFieldMetaData but for tables.

**Use Cases:**
- Table-level feature flags
- Custom caching configuration
- Integration settings

## Backend Modules

### QBackendModuleInterface

**Location:** `com.kingsrook.qqq.backend.core.modules`

**Registration:** Via QInstance backend configuration

**Methods:**

| Method | Description |
|--------|-------------|
| `getBackendType()` | Unique backend type name |
| `getQueryInterface()` | Query operation support |
| `getInsertInterface()` | Insert operation support |
| `getUpdateInterface()` | Update operation support |
| `getDeleteInterface()` | Delete operation support |
| `openTransaction()` | Transaction support |

**Use Cases:**
- Custom storage backends
- External API integrations
- Specialized data sources

## Metadata Production

### MetaDataProducerInterface<T>

**Location:** `com.kingsrook.qqq.backend.core.model.metadata`

**Registration:** Automatic via classpath scanning in QBit's package

**Methods:**

| Method | Description |
|--------|-------------|
| `T produce(QInstance)` | Produce metadata object |
| `getSortOrder()` | Production order (lower = earlier) |
| `isEnabled()` | Whether to produce |

**Use Cases:**
- Produce tables, processes, widgets, reports
- Dynamic metadata generation

### MetaDataCustomizerInterface<T>

**Location:** `com.kingsrook.qqq.backend.core.model.metadata`

**Registration:** Via QBitConfig or per-producer

**Methods:**

| Method | Description |
|--------|-------------|
| `T customizeMetaData(QInstance, T)` | Modify metadata after production |

**Use Cases:**
- Apply backend-specific settings
- Override default values
- Add security rules

## QBit-Specific

### QBitComponentMetaDataProducerInterface<T, C>

**Location:** `com.kingsrook.qqq.backend.core.model.metadata.qbits`

Extends MetaDataProducerInterface with QBitConfig access.

**Additional Methods:**

| Method | Description |
|--------|-------------|
| `C getQBitConfig()` | Access QBit configuration |
| `void setQBitConfig(C)` | Set configuration (called by framework) |

**Use Cases:**
- Access QBit config during metadata production
- Conditional metadata based on configuration
