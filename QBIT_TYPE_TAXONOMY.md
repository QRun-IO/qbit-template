# QBit Type Taxonomy and MCP RAG Plan

## Executive Summary

This document formalizes three distinct QBit types and outlines how the MCP RAG should support designing and creating new QBits. The goal is to enable AI-assisted QBit development with type-aware templates, validation, and guidance.

---

## QBit Type Taxonomy

### Type 1: Extension QBits

**Purpose:** Extend or enhance how QQQ operates at the infrastructure/framework level.

**Characteristics:**
- Provide new backend implementations, authentication mechanisms, or audit capabilities
- Minimal to no user-facing tables (purely operational)
- Often require specific infrastructure (databases, external services)
- Define new QQQ behaviors, validators, or action handlers
- Configuration-heavy; no `QAppSection`

**Examples:**
- RDBMS backend modules (MySQL, PostgreSQL, Aurora)
- HIPAA Audit WORM storage compliance
- SSO/SAML authentication providers
- Encryption-at-rest modules
- Custom logging/telemetry exporters

---

### Type 2: Data QBits

**Purpose:** Provide reusable reference data sets that applications can consume.

**Characteristics:**
- Primarily read-only or periodically-updated tables
- Include ETL processes for data refresh/sync
- Provide PossibleValueSources for lookups
- Tables are joined/referenced by application tables
- May include data versioning/timestamping
- Optional admin UI for data management

**Examples:**
- Geographic data (countries, states, cities, zip codes)
- Industry standard codes (SIC, NAICS, HS codes)
- Currency and exchange rate data
- Time zones and holiday calendars
- Product categorization taxonomies (UNSPSC)

---

### Type 3: Application QBits

**Purpose:** Provide complete, self-contained applications or major functional modules.

**Characteristics:**
- Multiple interconnected tables, processes, and widgets
- Full CRUD operations with business logic
- Include `QAppSection` for UI navigation
- Own security/permission model
- Designed to run standalone OR integrate with other QBits
- Composable (e.g., WMS + OMS integration points)

**Examples:**
- Warehouse Management System (WMS)
- Order Management System (OMS)
- Transportation Management System (TMS)
- Customer Relationship Management (CRM)
- Content Management System (CMS)
- Inventory Management
- Workflows/BPM engine (existing: `qbit-workflows`)

---

## QBit Composition Patterns

| Pattern | Description | Example |
|---------|-------------|---------|
| QBit depends on QBit | One QBit references tables from another | OMS uses WMS inventory tables |
| Application uses Data | App QBit references data QBit for lookups | CRM uses geographic data for addresses |
| Extension enhances Application | Extension wraps app tables with new behavior | HIPAA audit wraps patient tables |

---

## Architecture Patterns

### Table Prefixing Strategy

Data and Application QBits support table prefixing for multi-instance deployment. When an application needs the same QBit data for different purposes (shipping addresses vs billing addresses), each instance uses a unique prefix.

**Implementation:**
```java
// QBitConfig stores prefix
private String tableNamePrefix;  // e.g., "shipping" or "billing"

public String applyPrefix(String tableName)
{
   if(StringUtils.hasContent(tableNamePrefix))
   {
      return tableNamePrefix + "_" + tableName;
   }
   return tableName;
}
```

**Result:**
- No prefix: `country`, `state_province`, `city`
- With "shipping" prefix: `shipping_country`, `shipping_state_province`, `shipping_city`
- With "billing" prefix: `billing_country`, `billing_state_province`, `billing_city`

**Prefix application points:**
1. Table name in `QTableMetaData`
2. Foreign key references between tables
3. PossibleValueSource names (auto-generated)
4. Process names (if table-specific)

---

### Scope Tracking via SourceQBitAware

QQQ tracks which QBit produced each piece of metadata using the `SourceQBitAware` interface. This enables:
- Admin UI filtering by QBit
- Dependency analysis between QBits
- Scoped queries within a QBit's tables

**Implementation in Producer:**
```java
for(MetaDataProducerInterface<?> producer : producers)
{
   MetaDataProducerOutput output = producer.produce(qInstance);

   // Mark output with source QBit
   if(output instanceof SourceQBitAware sqa)
   {
      sqa.setSourceQBitName(qBitMetaData.getName());
   }

   output.addSelfToInstance(qInstance);
}
```

**What implements SourceQBitAware:**
- `QTableMetaData`
- `QProcessMetaData`
- `QPossibleValueSource`
- `QWidgetMetaData`

---

### Multi-Instance Usage Pattern

A single QBit can be instantiated multiple times with different configurations:

```java
// Instance 1: Shipping addresses
new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("shipping"))
   .produce(qInstance, "shipping-geo");

// Instance 2: Billing addresses
new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("billing"))
   .produce(qInstance, "billing-geo");

// Instance 3: Warehouse locations
new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("warehouse"))
   .produce(qInstance, "warehouse-geo");
```

**Key requirements for multi-instance support:**
1. All table/process/PVS names must use the prefix
2. Each instance must have a unique namespace
3. Cross-instance references require explicit configuration

---

### Data Sync Process Architecture

Data QBits include a sync process for loading/updating reference data. The process uses natural key matching for upsert operations.

**Pattern:**
```java
public class GeoDataSyncStep extends AbstractTransformStep
{
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output)
   {
      String prefix = getPrefix(input);

      // 1. Load embedded JSON data
      List<QRecord> sourceRecords = loadFromClasspath("/data/countries.json");

      // 2. Query existing by natural key
      Map<String, QRecord> existingByKey = queryExistingRecords(
         input, prefix + "_country", "alpha2Code");

      // 3. Categorize: insert, update, deactivate
      for(QRecord source : sourceRecords)
      {
         String key = source.getValueString("alpha2Code");
         QRecord existing = existingByKey.get(key);

         if(existing == null)
         {
            // Insert new record
            insertRecords.add(source);
         }
         else if(hasChanges(source, existing))
         {
            // Update changed record
            source.setValue("id", existing.getValue("id"));
            updateRecords.add(source);
         }
         existingByKey.remove(key);
      }

      // 4. Remaining in map are no longer in source - deactivate
      for(QRecord orphan : existingByKey.values())
      {
         orphan.setValue("isActive", false);
         deactivateRecords.add(orphan);
      }

      // 5. Execute operations
      insertRecords(insertRecords);
      updateRecords(updateRecords);
      updateRecords(deactivateRecords);

      // 6. Log summary
      LOG.info("Sync complete",
         logPair("inserted", insertRecords.size()),
         logPair("updated", updateRecords.size()),
         logPair("deactivated", deactivateRecords.size()));
   }
}
```

**Natural key design:**
| Table | Natural Key | Notes |
|-------|-------------|-------|
| country | `alpha2Code` | ISO 3166-1 alpha-2 |
| stateProvince | `countryId` + `code` | Composite key |
| city | `stateProvinceId` + `name` | May have duplicates across states |

---

### Liquibase Generation Pattern

Data QBits provide a Liquibase changelog generator that respects configuration (prefix, enabled tables).

**Template approach:**
1. Ship `changelog-template.xml` with placeholder tokens
2. Generator reads template, substitutes values, outputs final changelog
3. Host application includes generated changelog in their master changelog

**Template example:**
```xml
<changeSet id="${prefix}-create-country" author="geo-qbit">
   <createTable tableName="${prefix}_country">
      <column name="id" type="INT" autoIncrement="true">
         <constraints primaryKey="true"/>
      </column>
      <column name="alpha2_code" type="VARCHAR(2)">
         <constraints nullable="false" unique="true"/>
      </column>
      <column name="name" type="VARCHAR(100)"/>
      <column name="is_active" type="BOOLEAN" defaultValueBoolean="true"/>
   </createTable>
</changeSet>
```

**Generator:**
```java
public class GeoLiquibaseGenerator
{
   public static void generate(GeoDataQBitConfig config, Path outputPath)
   {
      String template = readResource("/db/changelog-template.xml");
      String prefix = config.getTableNamePrefix();

      // Substitute prefix
      String changelog = template.replace("${prefix}",
         prefix != null ? prefix : "geo");

      // Remove disabled table sections
      if(!config.isEnableCities())
      {
         changelog = removeSection(changelog, "city");
      }

      Files.writeString(outputPath, changelog);
   }
}
```

**Usage in host application:**
```xml
<!-- master-changelog.xml -->
<include file="generated/geo-shipping-changelog.xml"/>
<include file="generated/geo-billing-changelog.xml"/>
```

---

## MCP RAG Enhancement Plan

### Phase 1: Type-Aware Search and Discovery

**Goal:** MCP should understand QBit types and guide users to appropriate patterns.

**Actions:**
- Add `qbit_type` metadata field to indexed QBit chunks (extension/data/application)
- Update `search_qqq` to filter by QBit type
- Update `find_qbit` to return type classification
- Add `list_qbits_by_type` action

---

### Phase 2: QBit Creation Guidance

**Goal:** Provide step-by-step guidance for creating new QBits based on type.

**New MCP Actions:**

| Action | Purpose |
|--------|---------|
| `guide_qbit_creation` | Interactive wizard that asks clarifying questions and recommends type |
| `get_qbit_checklist` | Returns required/optional components for a given QBit type |
| `validate_qbit_design` | Validates proposed QBit structure against type requirements |

---

### Phase 3: Template Patterns

**Goal:** Store curated patterns for each QBit type that MCP can retrieve and adapt.

**Pattern Categories:**
- `extension-qbit-pattern` - Base structure for extension QBits
- `data-qbit-pattern` - Base structure with sync process and PVS
- `application-qbit-pattern` - Full app with sections, tables, processes
- `qbit-table-pattern` - How to add tables to any QBit type
- `qbit-process-pattern` - How to add processes to any QBit type
- `qbit-integration-pattern` - How to integrate multiple QBits

---

### Phase 4: Scaffolding Support

**Goal:** Generate boilerplate code for new QBits.

**Approach Options:**
1. **MCP-generated templates** - MCP returns template code with placeholders
2. **qctl integration** - `qctl qbit create --type=application --name=my-app`
3. **Hybrid** - MCP guides, qctl generates

**Key Decisions Needed:**
- Where do templates live? (MCP server, separate repo, qctl)
- How are templates versioned with QQQ versions?
- How do we handle type-specific variations?

---

## Type-Specific Requirements

### Extension QBit Requirements

| Requirement | Status |
|-------------|--------|
| No QAppSection | Enforced |
| Config validation for infrastructure | Required |
| May extend QQQ interfaces | Optional |
| Tests for backend integration | Required |

### Data QBit Requirements

| Requirement | Status |
|-------------|--------|
| Backend name configuration | Required |
| Sync scheduler configuration | Optional |
| PossibleValueSource registration | Recommended |
| Data versioning fields | Recommended |
| Admin UI toggle | Optional |

### Application QBit Requirements

| Requirement | Status |
|-------------|--------|
| QAppSection with navigation | Required |
| At least one table | Required |
| Security lock support | Recommended |
| Integration point configuration | Recommended |
| Feature toggles | Recommended |

---

## Open Questions and Discussion

### 1. Type Enforcement

**Question:** Should we add a formal `QBitType` enum to the framework, or keep types as documentation/convention?

| Approach | Pros | Cons |
|----------|------|------|
| **Formal enum** | Framework can validate, MCP can query type directly, enforces consistency | Requires QQQ core changes, may be too rigid for edge cases |
| **Convention only** | Flexible, no core changes needed, types can evolve | No compile-time enforcement, relies on documentation |
| **Hybrid** | Optional `getQBitType()` method on `QBitMetaDataProducer` with default | Best of both - opt-in typing with flexibility |

**Recommendation:** Hybrid approach - add an optional method to `QBitMetaDataProducer` that returns a type, defaulting to `UNSPECIFIED`. Enables classification without forcing it.

---

### 2. Inter-QBit Dependencies

**Question:** How do we formally declare that one QBit depends on another?

**Current state:** Maven `<dependency>` handles classpath, but nothing at metadata level.

| Approach | What it enables |
|----------|-----------------|
| **Maven only** | Simple, already works, but no runtime awareness |
| **Metadata declaration** | `QBitMetaData.withDependsOn("com.kingsrook.qbits:geo-data")` - runtime validation, dependency graph |
| **Config validation** | QBitConfig validates that required tables/PVS exist in QInstance |

**Recommendation:** Config validation is already the pattern (see `UserRolePermissionsQBitConfig` validating user table exists). Formal dependency declaration could be additive for tooling/documentation but not strictly required.

---

### 3. Namespace Conventions

**Question:** Should Data QBits use a `data:` namespace prefix? Application QBits use their name?

**Context:** QBits can optionally namespace their metadata (e.g., `workflows:workflowStep` vs just `workflowStep`).

| Approach | Example table names |
|----------|---------------------|
| **No namespace** | `country`, `order`, `inventoryItem` |
| **Type-based prefix** | `data:country`, `app:order`, `ext:auditLog` |
| **QBit name prefix** | `geoData:country`, `oms:order`, `wms:inventoryItem` |
| **Optional/configurable** | User chooses via config whether to namespace |

**Recommendation:** QBit name prefix - it's clear where things come from without imposing artificial categories. Keep it optional via configuration.

---

### 4. Data Versioning

**Question:** How do Data QBits handle versioned reference data?

**Examples:**
- Country names change (Burma to Myanmar)
- ZIP codes get reassigned
- Currency codes deprecated (EUR replaced national currencies)

| Approach | Description |
|----------|-------------|
| **Effective dating** | `effectiveDate`/`expirationDate` fields, queries filter by current date |
| **Snapshot tables** | `country_v2024`, `country_v2025` - separate tables per version |
| **Replace-in-place** | Just update records, no history |
| **SCD Type 2** | Slowly changing dimension pattern with `isCurrent` flag |

**Recommendation:** Effective dating as the recommended pattern for Data QBits, but not enforced. Some reference data (like ZIP codes) may just replace-in-place depending on use case.

---

### 5. Testing Patterns

**Question:** What are the minimum test requirements per type?

**Current QQQ standard:** 70% instruction coverage, 90% class coverage.

**Type-specific considerations:**

| Type | Special testing needs |
|------|----------------------|
| **Extension** | Backend integration tests, mock infrastructure |
| **Data** | Sync process tests, data validation, PVS lookup tests |
| **Application** | End-to-end workflows, security lock tests, UI navigation |

**Recommendation:** Keep existing coverage requirements. Add recommended test patterns per type in documentation, but don't enforce type-specific minimums.

---

## Next Steps

1. **Validate taxonomy** - Review with team, confirm the three types cover all use cases
2. **Add type metadata** - Update MCP indexing to classify existing QBits
3. **Create patterns** - Write pattern documents for each type
4. **Build guidance actions** - Implement `guide_qbit_creation` and `get_qbit_checklist`
5. **Decide on scaffolding** - Choose template storage and generation approach

---

## Appendix: Current QBit Inventory

| QBit | Type | Notes |
|------|------|-------|
| qbit-workflows | Application | Full workflow engine |
| qbit-webhooks | Application | Webhook subscription/delivery |
| qbit-user-role-permissions | Extension | Auth/permission system |
| qbit-standard-process-trace | Extension | Process audit logging |
| qbit-customizable-table-views | Extension | UI personalization |
| qbit-sftp-data-integration | Extension | SFTP import/export |

*No pure Data QBits exist yet - geographic/reference data would be first.*

---

| Date | Version | Notes |
|------|---------|-------|
| 2025-12-25 | 0.1.0 | Initial taxonomy and plan |
| 2025-12-27 | 0.2.0 | Added Architecture Patterns: prefixing, scope, multi-instance, sync, Liquibase |
