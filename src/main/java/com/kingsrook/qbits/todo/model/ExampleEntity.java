/*******************************************************************************
 ** Example entity demonstrating the QRecordEntity pattern.
 **
 ** This is a placeholder entity - replace or remove it when customizing your QBit.
 *******************************************************************************/
package com.kingsrook.qbits.todo.model;


import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


/*******************************************************************************
 ** Example entity for the Todo QBit.
 **
 ** Replace this with your own entities or delete if building an extension QBit.
 *******************************************************************************/
@QMetaDataProducingEntity(produceTableMetaData = true, producePossibleValueSource = true)
public class ExampleEntity extends QRecordEntity
{
   public static final String TABLE_NAME = "example";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, maxLength = 100, label = "Name")
   private String name;

   @QField(maxLength = 500, label = "Description")
   private String description;



   /***************************************************************************
    ** Default constructor
    ***************************************************************************/
   public ExampleEntity()
   {
   }



   /***************************************************************************
    ** Getter for id
    ***************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /***************************************************************************
    ** Setter for id
    ***************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /***************************************************************************
    ** Fluent setter for id
    ***************************************************************************/
   public ExampleEntity withId(Integer id)
   {
      this.id = id;
      return this;
   }



   /***************************************************************************
    ** Getter for name
    ***************************************************************************/
   public String getName()
   {
      return name;
   }



   /***************************************************************************
    ** Setter for name
    ***************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /***************************************************************************
    ** Fluent setter for name
    ***************************************************************************/
   public ExampleEntity withName(String name)
   {
      this.name = name;
      return this;
   }



   /***************************************************************************
    ** Getter for description
    ***************************************************************************/
   public String getDescription()
   {
      return description;
   }



   /***************************************************************************
    ** Setter for description
    ***************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /***************************************************************************
    ** Fluent setter for description
    ***************************************************************************/
   public ExampleEntity withDescription(String description)
   {
      this.description = description;
      return this;
   }

}
