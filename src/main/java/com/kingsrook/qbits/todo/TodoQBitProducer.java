/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qbits.todo;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitProducer;


/*******************************************************************************
 ** Producer class for the Todo QBit.
 **
 ** The QBitProducer is the entry point for a QBit. When an application wants to
 ** use your QBit, they create an instance of this producer, configure it, and
 ** call produce() to add the QBit's metadata to their QInstance.
 **
 ** Example usage in an application:
 ** <pre>
 ** new TodoQBitProducer()
 **    .withTodoQBitConfig(new TodoQBitConfig()
 **       .withBackendName("myBackend")
 **       .withSomeFeatureEnabled(true))
 **    .produce(qInstance);
 ** </pre>
 **
 ** See docs/01-qbit-basics.md for more details on QBitProducer.
 *******************************************************************************/
public class TodoQBitProducer implements QBitProducer
{
   /////////////////////////////////////////////////////////////////////////////
   // Maven-style coordinates for this QBit. These uniquely identify the     //
   // QBit and are used for versioning, dependency management, and display.  //
   /////////////////////////////////////////////////////////////////////////////
   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "todo";
   public static final String VERSION     = "0.1.0";

   private TodoQBitConfig todoQBitConfig;



   /*******************************************************************************
    ** Produce the QBit's metadata and add it to the QInstance.
    **
    ** This method:
    ** 1. Creates QBitMetaData with Maven-style identification
    ** 2. Registers the QBit with the QInstance
    ** 3. Discovers and produces all component metadata (tables, processes, etc.)
    ** 4. Applies configuration and customizations
    **
    ** @param qInstance the QInstance to add metadata to
    ** @param namespace optional namespace for multiple instances of this QBit
    *******************************************************************************/
   @Override
   public void produce(QInstance qInstance, String namespace) throws QException
   {
      //////////////////////////////////////////////////////////////////////////
      // Create QBitMetaData - this identifies the QBit and stores its config //
      //////////////////////////////////////////////////////////////////////////
      QBitMetaData qBitMetaData = new QBitMetaData()
         .withGroupId(GROUP_ID)
         .withArtifactId(ARTIFACT_ID)
         .withVersion(VERSION)
         .withNamespace(namespace)
         .withConfig(todoQBitConfig);

      ////////////////////////////////////
      // Register the QBit with QInstance //
      ////////////////////////////////////
      qInstance.addQBit(qBitMetaData);

      //////////////////////////////////////////////////////////////////////////
      // Find all MetaDataProducers in this package (entities, processes, etc.) //
      // MetaDataProducerHelper scans for:                                      //
      //   - Classes implementing MetaDataProducerInterface                     //
      //   - Classes annotated with @QMetaDataProducingEntity                   //
      //   - Classes annotated with @QMetaDataProducingPossibleValueEnum        //
      //////////////////////////////////////////////////////////////////////////
      List<MetaDataProducerInterface<?>> producers = MetaDataProducerHelper.findProducers(getClass().getPackageName());

      //////////////////////////////////////////////////////////////////////////
      // Example: Conditionally filter producers based on configuration       //
      // Uncomment and modify as needed                                        //
      //////////////////////////////////////////////////////////////////////////
      // if(!Boolean.TRUE.equals(todoQBitConfig.getSomeFeatureEnabled()))
      // {
      //    producers.removeIf(p -> p.getSourceClass().equals(SomeOptionalEntity.class));
      // }

      //////////////////////////////////////////////////////////////////////////
      // Finish producing - this processes all discovered producers and       //
      // applies the QBit's configuration to produced metadata                //
      //////////////////////////////////////////////////////////////////////////
      finishProducing(qInstance, qBitMetaData, todoQBitConfig, producers);
   }



   /*******************************************************************************
    ** Getter for todoQBitConfig
    *******************************************************************************/
   public TodoQBitConfig getTodoQBitConfig()
   {
      return (this.todoQBitConfig);
   }



   /*******************************************************************************
    ** Setter for todoQBitConfig
    *******************************************************************************/
   public void setTodoQBitConfig(TodoQBitConfig todoQBitConfig)
   {
      this.todoQBitConfig = todoQBitConfig;
   }



   /*******************************************************************************
    ** Fluent setter for todoQBitConfig
    *******************************************************************************/
   public TodoQBitProducer withTodoQBitConfig(TodoQBitConfig todoQBitConfig)
   {
      this.todoQBitConfig = todoQBitConfig;
      return (this);
   }

}
