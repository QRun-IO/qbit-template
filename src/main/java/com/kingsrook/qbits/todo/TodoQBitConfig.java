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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Configuration class for the Todo QBit.
 **
 ** QBitConfig provides configuration options that control how the QBit behaves
 ** when it is produced and at runtime. Configuration is validated during QBit
 ** production to catch errors early.
 **
 ** Common configuration patterns include:
 ** - Feature flags to enable/disable optional functionality
 ** - Backend names for where tables should be stored
 ** - Table customizers for modifying produced metadata
 ** - Integration settings (API keys, endpoints, etc.)
 **
 ** See docs/01-qbit-basics.md for more details on QBitConfig.
 *******************************************************************************/
public class TodoQBitConfig implements QBitConfig
{
   ///////////////////////////////////////////////////////////////////////////
   // Example configuration field - the backend name for tables.           //
   // Applications can override this to store QBit tables in their backend //
   ///////////////////////////////////////////////////////////////////////////
   private String backendName;

   ///////////////////////////////////////////////////////////////////////////
   // Example feature flag - enable or disable optional functionality      //
   ///////////////////////////////////////////////////////////////////////////
   private Boolean someFeatureEnabled = true;

   ///////////////////////////////////////////////////////////////////////////
   // Table customizer - allows applications to modify table metadata       //
   ///////////////////////////////////////////////////////////////////////////
   private MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer;



   /*******************************************************************************
    ** Validate the configuration.
    **
    ** This method is called during QBit production. Add any validation logic here
    ** to ensure the configuration is valid before the QBit is produced.
    **
    ** @param qInstance the QInstance being configured
    ** @param errors list to add validation error messages to
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      //////////////////////////////////////////////////////////////////////////
      // Example validation: require backendName if someFeatureEnabled       //
      // Uncomment and modify as needed for your QBit's requirements         //
      //////////////////////////////////////////////////////////////////////////
      // if(Boolean.TRUE.equals(someFeatureEnabled) && backendName == null)
      // {
      //    errors.add("backendName is required when someFeatureEnabled is true");
      // }
   }



   /*******************************************************************************
    ** Return the default backend name for tables produced by this QBit.
    **
    ** This tells QQQ which backend to use for storing data in the QBit's tables.
    ** Applications pass this value when configuring the QBit.
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Setter for backendName
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public TodoQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for someFeatureEnabled
    *******************************************************************************/
   public Boolean getSomeFeatureEnabled()
   {
      return (this.someFeatureEnabled);
   }



   /*******************************************************************************
    ** Setter for someFeatureEnabled
    *******************************************************************************/
   public void setSomeFeatureEnabled(Boolean someFeatureEnabled)
   {
      this.someFeatureEnabled = someFeatureEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for someFeatureEnabled
    *******************************************************************************/
   public TodoQBitConfig withSomeFeatureEnabled(Boolean someFeatureEnabled)
   {
      this.someFeatureEnabled = someFeatureEnabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableMetaDataCustomizer - overrides interface default.
    **
    ** Applications use this to customize table metadata during production,
    ** such as setting backend names or adding backend-specific details.
    *******************************************************************************/
   @Override
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (this.tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Setter for tableMetaDataCustomizer
    *******************************************************************************/
   public void setTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer
    *******************************************************************************/
   public TodoQBitConfig withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
      return (this);
   }

}
