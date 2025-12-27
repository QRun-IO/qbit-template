# QBit Basics

## What is a QBit?

A QBit is a modular, reusable component for the QQQ framework. QBits can provide:
- **Tables and entities** for data storage
- **Processes** for business logic
- **Extension hooks** that modify QQQ behavior
- **Complete applications** that combine all of the above

## Core Components

### QBitProducer

The producer is the entry point for your QBit. Applications use it to add your QBit to their QInstance:

```java
new MyQBitProducer()
   .withMyQBitConfig(new MyQBitConfig()
      .withBackendName("myBackend")
      .withSomeOption(true))
   .produce(qInstance);
```

Key responsibilities:
1. Create `QBitMetaData` with Maven-style identification
2. Register the QBit with the QInstance
3. Discover and produce all component metadata
4. Apply configuration and customizations

### QBitConfig

The config class holds configuration options that control how your QBit behaves:

```java
public class MyQBitConfig implements QBitConfig
{
   private String backendName;
   private Boolean someFeatureEnabled = true;

   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      // Validate configuration before QBit is produced
      if(backendName == null)
      {
         errors.add("backendName is required");
      }
   }

   // Getters, setters, fluent setters...
}
```

### QBitMetaData

Identifies your QBit using Maven-style coordinates:

```java
QBitMetaData qBitMetaData = new QBitMetaData()
   .withGroupId("com.kingsrook.qbits")
   .withArtifactId("my-feature")
   .withVersion("1.0.0")
   .withNamespace(namespace)  // Optional: allows multiple instances
   .withConfig(myConfig);
```

## Adding Entities

Use `@QMetaDataProducingEntity` to automatically generate table metadata:

```java
@QMetaDataProducingEntity(producePossibleValueSource = true)
public class MyEntity extends QRecordEntity
{
   public static final String TABLE_NAME = "myEntity";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   // Getters, setters, fluent setters...
}
```

The entity will be automatically discovered and produced when your QBit is produced.

## Adding Processes

Create a metadata producer for your process:

```java
public class MyProcessMetaDataProducer implements MetaDataProducerInterface<QProcessMetaData>
{
   @Override
   public QProcessMetaData produce(QInstance qInstance)
   {
      return new QProcessMetaData()
         .withName("myProcess")
         .withLabel("My Process")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withName("step1")
               .withCode(new QCodeReference(MyProcessStep.class))
         ));
   }
}
```

## Package Structure

Organize your QBit code:

```
com.kingsrook.qbits.myfeature/
├── MyFeatureQBitConfig.java
├── MyFeatureQBitProducer.java
├── model/
│   └── MyEntity.java
├── processes/
│   ├── MyProcessMetaDataProducer.java
│   └── MyProcessStep.java
├── actions/
│   └── MyCustomAction.java
└── customizers/
    └── MyTableCustomizer.java
```

## Testing

Extend `BaseTest` to get a configured QInstance:

```java
class MyEntityTest extends BaseTest
{
   @Test
   void testInsert() throws QException
   {
      InsertInput input = new InsertInput();
      input.setTableName(MyEntity.TABLE_NAME);
      input.setRecords(List.of(new QRecord().withValue("name", "Test")));

      InsertOutput output = new InsertAction().execute(input);
      assertThat(output.getRecords()).hasSize(1);
   }
}
```

## Next Steps

- [02-extension-patterns.md](02-extension-patterns.md) - Extend QQQ functionality
- [03-data-qbits.md](03-data-qbits.md) - Provide reference data
- [04-application-qbits.md](04-application-qbits.md) - Build complete applications
