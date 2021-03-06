/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.TenantEntity;
import org.thingsboard.server.dao.model.WidgetsBundleEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;

@Service
@Slf4j
public class WidgetsBundleServiceImpl implements WidgetsBundleService {

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Override
    public WidgetsBundle findWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetsBundleById [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        WidgetsBundleEntity widgetsBundleEntity = widgetsBundleDao.findById(widgetsBundleId.getId());
        return getData(widgetsBundleEntity);
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        log.trace("Executing saveWidgetsBundle [{}]", widgetsBundle);
        widgetsBundleValidator.validate(widgetsBundle);
        WidgetsBundleEntity widgetsBundleEntity = widgetsBundleDao.save(widgetsBundle);
        return getData(widgetsBundleEntity);
    }

    @Override
    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        log.trace("Executing deleteWidgetsBundle [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        WidgetsBundle widgetsBundle = findWidgetsBundleById(widgetsBundleId);
        if (widgetsBundle == null) {
            throw new IncorrectParameterException("Unable to delete non-existent widgets bundle.");
        }
        widgetTypeService.deleteWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias());
        widgetsBundleDao.removeById(widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias) {
        log.trace("Executing findWidgetsBundleByTenantIdAndAlias, tenantId [{}], alias [{}]", tenantId, alias);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateString(alias, "Incorrect alias " + alias);
        WidgetsBundleEntity widgetsBundleEntity = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(tenantId.getId(), alias);
        return getData(widgetsBundleEntity);
    }

    @Override
    public TextPageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TextPageLink pageLink) {
        log.trace("Executing findSystemWidgetsBundles, pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = widgetsBundleDao.findSystemWidgetsBundles(pageLink);
        List<WidgetsBundle> widgetsBundles = convertDataList(widgetsBundlesEntities);
        return new TextPageData<>(widgetsBundles, pageLink);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles() {
        log.trace("Executing findSystemWidgetsBundles");
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<WidgetsBundle> pageData = null;
        do {
            pageData = findSystemWidgetsBundlesByPageLink(pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public TextPageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
        List<WidgetsBundle> widgetsBundles = convertDataList(widgetsBundlesEntities);
        return new TextPageData<>(widgetsBundles, pageLink);
    }

    @Override
    public TextPageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<WidgetsBundleEntity> widgetsBundlesEntities = widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
        List<WidgetsBundle> widgetsBundles = convertDataList(widgetsBundlesEntities);
        return new TextPageData<>(widgetsBundles, pageLink);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<WidgetsBundle> pageData = null;
        do {
            pageData = findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public void deleteWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        tenantWidgetsBundleRemover.removeEntitites(tenantId);
    }

    private DataValidator<WidgetsBundle> widgetsBundleValidator =
            new DataValidator<WidgetsBundle>() {

                @Override
                protected void validateDataImpl(WidgetsBundle widgetsBundle) {
                    if (StringUtils.isEmpty(widgetsBundle.getTitle())) {
                        throw new DataValidationException("Widgets bundle title should be specified!");
                    }
                    if (widgetsBundle.getTenantId() == null) {
                        widgetsBundle.setTenantId(new TenantId(ModelConstants.NULL_UUID));
                    }
                    if (!widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
                        TenantEntity tenant = tenantDao.findById(widgetsBundle.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Widgets bundle is referencing to non-existent tenant!");
                        }
                    }
                }

                @Override
                protected void validateCreate(WidgetsBundle widgetsBundle) {
                    String alias = widgetsBundle.getTitle().toLowerCase().replaceAll("\\W+", "_");
                    String originalAlias = alias;
                    int c = 1;
                    WidgetsBundleEntity withSameAlias;
                    do {
                        withSameAlias = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(widgetsBundle.getTenantId().getId(), alias);
                        if (withSameAlias != null) {
                            alias = originalAlias + (++c);
                        }
                    } while(withSameAlias != null);
                    widgetsBundle.setAlias(alias);
                }

                @Override
                protected void validateUpdate(WidgetsBundle widgetsBundle) {
                    WidgetsBundleEntity storedWidgetsBundle = widgetsBundleDao.findById(widgetsBundle.getId().getId());
                    if (!storedWidgetsBundle.getTenantId().equals(widgetsBundle.getTenantId().getId())) {
                        throw new DataValidationException("Can't move existing widgets bundle to different tenant!");
                    }
                    if (!storedWidgetsBundle.getAlias().equals(widgetsBundle.getAlias())) {
                        throw new DataValidationException("Update of widgets bundle alias is prohibited!");
                    }
                }

            };

    private PaginatedRemover<TenantId, WidgetsBundleEntity> tenantWidgetsBundleRemover =
            new PaginatedRemover<TenantId, WidgetsBundleEntity>() {

                @Override
                protected List<WidgetsBundleEntity> findEntities(TenantId id, TextPageLink pageLink) {
                    return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(WidgetsBundleEntity entity) {
                    deleteWidgetsBundle(new WidgetsBundleId(entity.getId()));
                }
            };

}
