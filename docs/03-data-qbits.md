# Data QBits

Data QBits provide reusable reference data such as countries, currencies, time zones, or industry-specific lookup tables.

## Structure

A data QBit typically includes:
- Entity classes for the reference data
- Seed data files (CSV, JSON, or SQL)
- Optional data loading logic

```
com.kingsrook.qbits.countries/
├── CountriesQBitConfig.java
├── CountriesQBitProducer.java
├── model/
│   └── Country.java
├── loaders/
│   └── CountryDataSeeder.java
└── resources/
    └── data/
        └── countries.csv
```

## Entity Definition

```java
@QMetaDataProducingEntity(producePossibleValueSource = true)
public class Country extends QRecordEntity
{
   public static final String TABLE_NAME = "country";

   @QField(isPrimaryKey = true, maxLength = 2)
   private String isoCode2;

   @QField(maxLength = 3)
   private String isoCode3;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   @QField(maxLength = 50)
   private String region;

   // Getters, setters, fluent setters...
}
```

## Seed Data (CSV)

Create `src/main/resources/data/countries.csv`:

```csv
isoCode2,isoCode3,name,region
US,USA,United States,North America
CA,CAN,Canada,North America
MX,MEX,Mexico,North America
GB,GBR,United Kingdom,Europe
DE,DEU,Germany,Europe
FR,FRA,France,Europe
```

## Data Seeder

```java
public class CountryDataSeeder
{
   public void seedData(QInstance qInstance, String backendName) throws QException
   {
      InputStream csvStream = getClass().getResourceAsStream("/data/countries.csv");
      if(csvStream == null)
      {
         throw new QException("countries.csv not found");
      }

      List<QRecord> records = parseCsv(csvStream);

      InsertInput input = new InsertInput();
      input.setTableName(Country.TABLE_NAME);
      input.setRecords(records);

      new InsertAction().execute(input);
   }

   private List<QRecord> parseCsv(InputStream stream)
   {
      // Parse CSV and return QRecords
      // Use OpenCSV or similar library
   }
}
```

## Configuration Options

```java
public class CountriesQBitConfig implements QBitConfig
{
   private String backendName;
   private Boolean autoSeedOnStartup = false;
   private Boolean includeAllCountries = true;
   private List<String> regionsToInclude;  // Filter by region

   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      if(autoSeedOnStartup && backendName == null)
      {
         errors.add("backendName required when autoSeedOnStartup is true");
      }
   }

   // Getters, setters, fluent setters...
}
```

## Producer with Seeding

```java
@Override
public void produce(QInstance qInstance, String namespace) throws QException
{
   // ... standard QBit production ...

   // Optionally seed data after production
   if(Boolean.TRUE.equals(countriesQBitConfig.getAutoSeedOnStartup()))
   {
      // Register a post-startup action or use a process
      // Note: Seeding should typically be done by the application,
      // not automatically, to avoid duplicate data on restart
   }
}
```

## Application Usage

```java
// Use the QBit with auto-seed disabled (default)
new CountriesQBitProducer()
   .withCountriesQBitConfig(new CountriesQBitConfig()
      .withBackendName("myBackend"))
   .produce(qInstance);

// Application seeds data during setup/migration
new CountryDataSeeder().seedData(qInstance, "myBackend");
```

## Best Practices

1. **Don't auto-seed by default** - Let applications control when data is seeded
2. **Provide upsert logic** - Handle re-seeding without duplicates
3. **Version your data** - Include data version in config or resources
4. **Support filtering** - Let applications include only the data they need
5. **Use Possible Value Sources** - Enable dropdown selection in UI
