# Data QBits

Data QBits provide reusable reference data sets that applications consume. They support multi-instance deployment with table prefixing, natural key-based sync for updates, and Liquibase changelog generation.

## Key Characteristics

| Feature | Description |
|---------|-------------|
| Table prefixing | Each instance uses configurable prefix (`shipping_country`, `billing_country`) |
| Multi-instance | Same QBit deployed multiple times in one app |
| Sync process | Upsert by natural key (insert new, update changed, deactivate removed) |
| Liquibase generator | Template-based changelog respecting prefix and enabled tables |
| PossibleValueSources | Auto-generated, prefix-aware |

---

## Structure

```
com.kingsrook.qbits.geodata/
├── GeoDataQBitConfig.java          # Extends AbstractDataQBitConfig
├── GeoDataQBitProducer.java        # Extends AbstractDataQBitProducer
├── model/
│   ├── Country.java
│   ├── StateProvince.java
│   └── City.java
├── sync/
│   └── GeoDataSyncProcess.java     # Upsert by natural key
├── liquibase/
│   └── GeoLiquibaseGenerator.java
└── resources/
    ├── data/
    │   ├── countries.json
    │   ├── state-provinces.json
    │   └── cities.json
    └── db/
        └── changelog-template.xml
```

---

## Table Prefixing

### Configuration

```java
public class GeoDataQBitConfig extends AbstractDataQBitConfig
{
   private Boolean enableCountries = true;
   private Boolean enableStateProvinces = true;
   private Boolean enableCities = true;
   private List<String> countryFilter;  // Limit to specific alpha2 codes

   @Override
   public List<String> getEnabledTableNames()
   {
      List<String> tables = new ArrayList<>();
      if(Boolean.TRUE.equals(enableCountries))
      {
         tables.add(Country.TABLE_NAME);
      }
      if(Boolean.TRUE.equals(enableStateProvinces))
      {
         tables.add(StateProvince.TABLE_NAME);
      }
      if(Boolean.TRUE.equals(enableCities))
      {
         tables.add(City.TABLE_NAME);
      }
      return tables;
   }
}
```

### Usage

```java
// Single instance, no prefix
new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms"))
   .produce(qInstance, "geo");
// Tables: country, state_province, city

// Multi-instance with prefixes
new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("shipping"))
   .produce(qInstance, "shipping-geo");
// Tables: shipping_country, shipping_state_province, shipping_city

new GeoDataQBitProducer()
   .withConfig(new GeoDataQBitConfig()
      .withBackendName("rdbms")
      .withTableNamePrefix("billing")
      .withEnableCities(false))  // Skip cities for billing
   .produce(qInstance, "billing-geo");
// Tables: billing_country, billing_state_province (no cities)
```

---

## Natural Key Design

Each table defines a natural key for upsert matching:

| Table | Natural Key Fields | Example |
|-------|-------------------|---------|
| country | `alpha2Code` | "US", "CA", "MX" |
| stateProvince | `countryId`, `code` | (1, "CA"), (1, "TX") |
| city | `stateProvinceId`, `name` | (5, "Los Angeles") |

### Entity with Natural Key

```java
@QMetaDataProducingEntity(producePossibleValueSource = true)
public class Country extends QRecordEntity
{
   public static final String TABLE_NAME = "country";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 2)
   private String alpha2Code;  // Natural key

   @QField(maxLength = 3)
   private String alpha3Code;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   @QField
   private Boolean isActive = true;  // Soft delete flag

   // Fluent setters...
}
```

---

## Sync Process

The sync process loads embedded JSON and performs upsert by natural key:

```java
public class GeoDataSyncStep extends AbstractTransformStep
{
   private static final QLogger LOG = QLogger.getLogger(GeoDataSyncStep.class);

   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output)
      throws QException
   {
      String tableName = input.getValueString("tableName");
      String naturalKeyField = input.getValueString("naturalKeyField");

      // 1. Load source data from classpath
      List<QRecord> sourceRecords = loadJsonData(tableName);

      // 2. Query existing by natural key
      Map<String, QRecord> existingByKey = queryExisting(input, tableName, naturalKeyField);

      // 3. Categorize records
      List<QRecord> toInsert = new ArrayList<>();
      List<QRecord> toUpdate = new ArrayList<>();
      List<QRecord> toDeactivate = new ArrayList<>();

      for(QRecord source : sourceRecords)
      {
         String key = source.getValueString(naturalKeyField);
         QRecord existing = existingByKey.remove(key);

         if(existing == null)
         {
            toInsert.add(source);
         }
         else if(hasChanges(source, existing))
         {
            source.setValue("id", existing.getValue("id"));
            toUpdate.add(source);
         }
      }

      // Remaining records no longer in source
      for(QRecord orphan : existingByKey.values())
      {
         orphan.setValue("isActive", false);
         toDeactivate.add(orphan);
      }

      // 4. Execute operations
      insertRecords(input, tableName, toInsert);
      updateRecords(input, tableName, toUpdate);
      updateRecords(input, tableName, toDeactivate);

      // 5. Log summary
      LOG.info("Sync complete",
         logPair("table", tableName),
         logPair("inserted", toInsert.size()),
         logPair("updated", toUpdate.size()),
         logPair("deactivated", toDeactivate.size()));
   }
}
```

### Data File Format

```json
[
   {
      "alpha2Code": "US",
      "alpha3Code": "USA",
      "name": "United States",
      "numericCode": 840
   },
   {
      "alpha2Code": "CA",
      "alpha3Code": "CAN",
      "name": "Canada",
      "numericCode": 124
   }
]
```

---

## Liquibase Generation

### Template File

Ship `src/main/resources/db/changelog-template.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

   <!-- SECTION: country -->
   <changeSet id="${prefix}-create-country-v1" author="geo-qbit">
      <createTable tableName="${prefix}_country">
         <column name="id" type="INT" autoIncrement="true">
            <constraints primaryKey="true"/>
         </column>
         <column name="alpha2_code" type="VARCHAR(2)">
            <constraints nullable="false" unique="true"/>
         </column>
         <column name="alpha3_code" type="VARCHAR(3)"/>
         <column name="name" type="VARCHAR(100)">
            <constraints nullable="false"/>
         </column>
         <column name="is_active" type="BOOLEAN" defaultValueBoolean="true"/>
      </createTable>
   </changeSet>
   <!-- END SECTION: country -->

   <!-- SECTION: stateProvince -->
   <changeSet id="${prefix}-create-state-province-v1" author="geo-qbit">
      <createTable tableName="${prefix}_state_province">
         <column name="id" type="INT" autoIncrement="true">
            <constraints primaryKey="true"/>
         </column>
         <column name="country_id" type="INT">
            <constraints nullable="false"/>
         </column>
         <column name="code" type="VARCHAR(10)">
            <constraints nullable="false"/>
         </column>
         <column name="name" type="VARCHAR(100)">
            <constraints nullable="false"/>
         </column>
         <column name="is_active" type="BOOLEAN" defaultValueBoolean="true"/>
      </createTable>
      <addForeignKeyConstraint
         baseTableName="${prefix}_state_province"
         baseColumnNames="country_id"
         referencedTableName="${prefix}_country"
         referencedColumnNames="id"
         constraintName="${prefix}_fk_sp_country"/>
   </changeSet>
   <!-- END SECTION: stateProvince -->

</databaseChangeLog>
```

### Generator

```java
public class GeoLiquibaseGenerator
{
   public static void generate(GeoDataQBitConfig config, Path outputPath)
      throws IOException
   {
      String template = readResource("/db/changelog-template.xml");
      String prefix = config.getTableNamePrefix();

      if(prefix == null)
      {
         prefix = "geo";
      }

      // Substitute prefix
      String changelog = template.replace("${prefix}", prefix);

      // Remove disabled sections
      if(!Boolean.TRUE.equals(config.getEnableStateProvinces()))
      {
         changelog = removeSection(changelog, "stateProvince");
      }
      if(!Boolean.TRUE.equals(config.getEnableCities()))
      {
         changelog = removeSection(changelog, "city");
      }

      Files.writeString(outputPath, changelog);
   }

   private static String removeSection(String content, String sectionName)
   {
      String startMarker = "<!-- SECTION: " + sectionName + " -->";
      String endMarker = "<!-- END SECTION: " + sectionName + " -->";
      // Remove content between markers
      return content.replaceAll(
         "(?s)" + Pattern.quote(startMarker) + ".*?" + Pattern.quote(endMarker),
         "");
   }
}
```

### Host Application Integration

```java
// Generate changelog during build or setup
GeoLiquibaseGenerator.generate(
   new GeoDataQBitConfig()
      .withTableNamePrefix("shipping")
      .withEnableCities(true),
   Path.of("src/main/resources/db/generated/shipping-geo-changelog.xml"));
```

```xml
<!-- Host app's master-changelog.xml -->
<databaseChangeLog>
   <include file="db/app-changelog.xml"/>
   <include file="db/generated/shipping-geo-changelog.xml"/>
   <include file="db/generated/billing-geo-changelog.xml"/>
</databaseChangeLog>
```

---

## Best Practices

1. **Always use natural keys** for sync - enables upsert without duplicates
2. **Include `isActive` field** for soft deletes during sync
3. **Use table prefixes** when multi-instance is possible
4. **Ship changelog template** not final changelog
5. **Make cities optional** (large dataset ~150k records)
6. **Filter by country** for regional deployments
7. **Version data files** for reproducible syncs

---

## See Also

- [ARCHITECTURE.md](ARCHITECTURE.md) - Base class hierarchy and patterns
- [QBIT_TYPE_TAXONOMY.md](../QBIT_TYPE_TAXONOMY.md) - Type taxonomy
- [01-qbit-basics.md](01-qbit-basics.md) - Core QBit concepts
