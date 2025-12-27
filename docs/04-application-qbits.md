# Application QBits

Application QBits are complete mini-applications that can be composed to build larger systems. Examples include WMS (Warehouse Management), CRM, Shipping, Inventory, and Newsletter systems.

## Structure

An application QBit includes:
- Multiple related entities
- Business processes
- Widgets and dashboards
- API endpoints (optional)
- Configuration options for customization

```
com.kingsrook.qbits.inventory/
├── InventoryQBitConfig.java
├── InventoryQBitProducer.java
├── model/
│   ├── Warehouse.java
│   ├── Location.java
│   ├── Item.java
│   ├── StockLevel.java
│   └── StockMovement.java
├── processes/
│   ├── ReceiveStockProcessMetaDataProducer.java
│   ├── ReceiveStockTransformStep.java
│   ├── TransferStockProcessMetaDataProducer.java
│   └── TransferStockTransformStep.java
├── widgets/
│   ├── StockLevelWidgetMetaDataProducer.java
│   └── LowStockAlertWidgetMetaDataProducer.java
├── customizers/
│   └── StockMovementTableCustomizer.java
└── api/
    └── InventoryApiMetaDataProducer.java
```

## Entity Relationships

Define parent-child relationships using annotations:

```java
@QMetaDataProducingEntity(
   producePossibleValueSource = true,
   childTables = {
      @ChildTable(
         childTableEntityClass = Location.class,
         joinFieldName = "warehouseId",
         childJoin = @ChildJoin(enabled = true)
      )
   }
)
public class Warehouse extends QRecordEntity
{
   public static final String TABLE_NAME = "warehouse";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100)
   private String name;

   @QField(maxLength = 50)
   private String code;

   // Getters, setters, fluent setters...
}
```

## Processes

Create processes for business operations:

```java
public class ReceiveStockProcessMetaDataProducer implements MetaDataProducerInterface<QProcessMetaData>
{
   @Override
   public QProcessMetaData produce(QInstance qInstance)
   {
      return new QProcessMetaData()
         .withName("receiveStock")
         .withLabel("Receive Stock")
         .withTableName(StockMovement.TABLE_NAME)
         .withStepList(List.of(
            new QFrontendStepMetaData()
               .withName("input")
               .withLabel("Enter Receipt Details")
               .withFormFields(List.of("itemId", "locationId", "quantity")),
            new QBackendStepMetaData()
               .withName("process")
               .withCode(new QCodeReference(ReceiveStockTransformStep.class))
         ));
   }
}
```

## Configuration for Extensibility

Allow applications to customize behavior:

```java
public class InventoryQBitConfig implements QBitConfig
{
   private String backendName;

   // Feature flags
   private Boolean enableMultiWarehouse = true;
   private Boolean enableSerialNumbers = false;
   private Boolean enableBatchTracking = false;

   // Integration hooks
   private QCodeReference onStockReceived;
   private QCodeReference onStockTransferred;
   private QCodeReference onLowStockAlert;

   // Thresholds
   private Integer lowStockThreshold = 10;

   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      if(backendName == null)
      {
         errors.add("backendName is required");
      }
   }

   // Getters, setters, fluent setters...
}
```

## Producer with Feature Flags

```java
@Override
public void produce(QInstance qInstance, String namespace) throws QException
{
   QBitMetaData qBitMetaData = new QBitMetaData()
      .withGroupId(GROUP_ID)
      .withArtifactId(ARTIFACT_ID)
      .withVersion(VERSION)
      .withNamespace(namespace)
      .withConfig(inventoryQBitConfig);

   qInstance.addQBit(qBitMetaData);

   List<MetaDataProducerInterface<?>> producers = MetaDataProducerHelper.findProducers(getClass().getPackageName());

   // Filter based on configuration
   if(!Boolean.TRUE.equals(inventoryQBitConfig.getEnableMultiWarehouse()))
   {
      producers.removeIf(p -> p.getSourceClass().equals(Warehouse.class));
   }

   if(!Boolean.TRUE.equals(inventoryQBitConfig.getEnableSerialNumbers()))
   {
      producers.removeIf(p -> p.getSourceClass().equals(SerialNumber.class));
   }

   finishProducing(qInstance, qBitMetaData, inventoryQBitConfig, producers);
}
```

## Application Usage

```java
// Basic usage
new InventoryQBitProducer()
   .withInventoryQBitConfig(new InventoryQBitConfig()
      .withBackendName("myBackend"))
   .produce(qInstance);

// Advanced usage with customization
new InventoryQBitProducer()
   .withInventoryQBitConfig(new InventoryQBitConfig()
      .withBackendName("myBackend")
      .withEnableSerialNumbers(true)
      .withEnableBatchTracking(true)
      .withLowStockThreshold(25)
      .withOnLowStockAlert(new QCodeReference(MyLowStockHandler.class)))
   .produce(qInstance);
```

## Composing Multiple QBits

Application QBits can be composed:

```java
// Build a complete system from QBits
new InventoryQBitProducer()
   .withInventoryQBitConfig(inventoryConfig)
   .produce(qInstance);

new ShippingQBitProducer()
   .withShippingQBitConfig(shippingConfig)
   .produce(qInstance);

new OrdersQBitProducer()
   .withOrdersQBitConfig(ordersConfig)
   .produce(qInstance);
```

## Best Practices

1. **Keep entities cohesive** - Group related entities in one QBit
2. **Use feature flags** - Allow applications to enable/disable features
3. **Provide extension hooks** - Let applications inject custom behavior
4. **Document dependencies** - If your QBit depends on another, document it
5. **Design for composition** - QBits should work together seamlessly
