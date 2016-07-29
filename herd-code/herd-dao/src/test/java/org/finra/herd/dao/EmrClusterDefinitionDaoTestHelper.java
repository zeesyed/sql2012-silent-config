/*
* Copyright 2015 herd contributors
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.finra.herd.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.finra.herd.model.jpa.EmrClusterDefinitionEntity;
import org.finra.herd.model.jpa.NamespaceEntity;

@Component
public class EmrClusterDefinitionDaoTestHelper
{
    @Autowired
    private EmrClusterDefinitionDao emrClusterDefinitionDao;

    /**
     * Creates and persists a new EMR cluster definition entity.
     *
     * @param namespaceEntity the namespace entity
     * @param definitionName the cluster definition name
     * @param configurationXml the cluster configuration XML
     *
     * @return the newly created job definition entity
     */
    public EmrClusterDefinitionEntity createEmrClusterDefinitionEntity(NamespaceEntity namespaceEntity, String definitionName, String configurationXml)
    {
        EmrClusterDefinitionEntity emrClusterDefinitionEntity = new EmrClusterDefinitionEntity();
        emrClusterDefinitionEntity.setNamespace(namespaceEntity);
        emrClusterDefinitionEntity.setName(definitionName);
        emrClusterDefinitionEntity.setConfiguration(configurationXml);
        return emrClusterDefinitionDao.saveAndRefresh(emrClusterDefinitionEntity);
    }
}
