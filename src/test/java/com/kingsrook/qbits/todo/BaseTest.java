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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Base test class for Todo QBit tests.
 **
 ** This class sets up the QInstance with the QBit for each test. Extend this
 ** class when writing tests that need the QBit's metadata available.
 **
 ** For QBits with database tables, you may want to add database priming logic
 ** using the RDBMS backend. See docs/01-qbit-basics.md for examples.
 *******************************************************************************/
public class BaseTest
{
   public static final String BACKEND_NAME = "testBackend";



   /*******************************************************************************
    ** Set up the QInstance with the QBit before each test.
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach() throws Exception
   {
      QInstance qInstance = defineQInstance();
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, new QSession());

      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Define the QInstance with the QBit.
    **
    ** This creates a minimal QInstance with:
    ** - Anonymous authentication (for testing)
    ** - An H2 in-memory database backend
    ** - The Todo QBit produced with default configuration
    **
    ** Customize this method as your QBit grows to include entities and processes.
    *******************************************************************************/
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = new QInstance();

      //////////////////////////////////////////////
      // Set up anonymous authentication for tests //
      //////////////////////////////////////////////
      qInstance.setAuthentication(new QAuthenticationMetaData()
         .withType(QAuthenticationType.FULLY_ANONYMOUS));

      ///////////////////////////////////
      // Add an H2 in-memory backend   //
      ///////////////////////////////////
      qInstance.addBackend(new RDBMSBackendMetaData()
         .withName(BACKEND_NAME)
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("test_database")
         .withUsername("sa"));

      ////////////////////////////////
      // Configure and produce QBit //
      ////////////////////////////////
      TodoQBitConfig config = new TodoQBitConfig()
         .withBackendName(BACKEND_NAME)
         .withSomeFeatureEnabled(true)
         .withTableMetaDataCustomizer((qInstance1, table) ->
         {
            if(table.getBackendName() == null)
            {
               table.setBackendName(BACKEND_NAME);
            }
            return (table);
         });

      new TodoQBitProducer()
         .withTodoQBitConfig(config)
         .produce(qInstance);

      return qInstance;
   }



   /*******************************************************************************
    ** Basic test to verify the QBit produces successfully.
    *******************************************************************************/
   @Test
   void testQBitProduces() throws QException
   {
      ///////////////////////////////////////////////////////
      // Verify the QBit was registered with the QInstance //
      ///////////////////////////////////////////////////////
      assertThat(QContext.getQInstance().getQBits()).isNotEmpty();
      assertThat(QContext.getQInstance().getQBits()).containsKey(
         TodoQBitProducer.GROUP_ID + ":" + TodoQBitProducer.ARTIFACT_ID);
   }

}
