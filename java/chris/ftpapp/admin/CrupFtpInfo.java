package chris.ftpapp.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.tapestry5.Field;
import org.apache.tapestry5.FieldValidator;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.FieldValidatorSource;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.RequestGlobals;

import chris.ftpapp.domain.Client;

import xvive.util.XUtil;

@RequiresSysAdmin
public class CrupFtpInfo {

  @Property
  @Persist
  private Client client;

  @Property
  @Persist
  private Boolean savedRptVisibleByInctypeFlag;

  @Property
  @Persist
  private Boolean savedRptVisibleByDistrictFlag;

  @Property
  @Persist
  private Boolean savedUseBicycleFields;

  @Property
  @Persist
  private Boolean savedUseJewelryFields;

  @Property
  @Persist
  private Boolean savedUseBoatFields;

  @Property
  @Persist
  private Boolean savedUseSecurityFields;

  @Property
  @Persist
  private Boolean savedUseAlcoholFields;
  
  @Property
  private Integer nextRptNo;

  @Property
  private Integer nextCadNo;

  @Property
  private Integer nextSeq3No;

  @Property
  private Integer maxDocSizeInMB;

  @Property
  private boolean usePropTypeCodex;
   
  @Property
  private String propTypeCodexLabel;

  @Property
  private boolean usePropSubtypeCodex;

  @Property
  private String propSubtypeCodexLabel;
  
  @Inject
  private FieldValidatorSource fvSource;

  @InjectComponent
  private Form myForm;

  @InjectComponent
  private Field propTypeCodexLabelField;

  @InjectComponent
  private Field propSubtypeCodexLabelField;

  @InjectComponent
  private Field agNameField;
  
  @InjectComponent
  private Field agRmsField;
  
  @InjectComponent
  private Field inputPhone;

  @InjectComponent
  private Field inputFax;

  @InjectComponent
  private Field rptSaleConvenienceFeeField;

  @Inject
  private DorsService dorsService;

  @Inject
  @Property
  private CacheService cacheService;

  @Inject
  private Messages messages;

  @Inject
  private RequestGlobals requestGlobals;

  @Inject
  private Request request;

  @SessionState
  @Property
  private DorsSessionState visit;

  @InjectComponent
  private Field agNotes;

  void setupRender() {
    visit.setActiveTab(DorsConst.TAB_AGENCY);

    agency = (Agency)dorsService.getById(Agency.class, visit.getAgencyId());

    savedRptVisibleByInctypeFlag = agency.getRptVisibleByInctype();
    savedRptVisibleByDistrictFlag = agency.getRptVisibleByDistrict();
    savedUseBicycleFields = agency.getUseBicycleFields();
    savedUseJewelryFields = agency.getUseJewelryFields();
    savedUseBoatFields = agency.getUseBoatFields();
    savedUseSecurityFields = agency.getUseSecurityFields();
    savedUseAlcoholFields = agency.getUseAlcoholFields();

  }

  void onPrepare() {
    // pre-fill codex-related
    List<RefDataTypeCodex> codexList = dorsService.getRefDataTypeCodexList(visit.getAgencyId());
    if (XUtil.isNotEmpty(codexList)) {
      for (RefDataTypeCodex rdtCodex : codexList) {
        if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_TYPE) {
          usePropTypeCodex = true;
          propTypeCodexLabel = rdtCodex.getCodexLabel();
        } else if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_SUBTYPE) {
          usePropSubtypeCodex = true;
          propSubtypeCodexLabel = rdtCodex.getCodexLabel();
        }
      }
    }
  }

  void onValidateFromMyForm() {
    if (DorsUtil.isTrue(usePropTypeCodex)) {
      if (XUtil.isEmpty(propTypeCodexLabel)) {
        myForm.recordError(propTypeCodexLabelField, messages.get("propTypeCodexLabel.required"));
      }
    }

    if (DorsUtil.isTrue(usePropSubtypeCodex)) {
      if (XUtil.isEmpty(propSubtypeCodexLabel)) {
        myForm.recordError(propSubtypeCodexLabelField, messages.get("propSubtypeCodexLabel.required"));
      }
    }

    if (agency.getRptSaleConvenienceFee() != null) {
      double value = agency.getRptSaleConvenienceFee().doubleValue();
      if (value < 0.01) {
        myForm.recordError(rptSaleConvenienceFeeField,
            messages.format("min-decimal", "0.01", rptSaleConvenienceFeeField.getLabel()));
        return;
      }
      if (value > 9999.99) {
        myForm.recordError(rptSaleConvenienceFeeField,
            messages.format("max-decimal", "9999.99", rptSaleConvenienceFeeField.getLabel()));
        return;
      }
    }

    // dup-check: (state + agency's name) should be unique.
    Agency agFromDb = dorsService.getAgency(agency.getState(), agency.getName());
    if (agFromDb != null && !agFromDb.getId().equals(agency.getId())) {
      myForm.recordError(agNameField, messages.get("agency-already-signedup"));
      return;
    }

  }


  Object onSuccess() {
    // codex 
    List<RefDataTypeCodex> codexList = dorsService.getRefDataTypeCodexList(visit.getAgencyId());
    if (usePropTypeCodex) {
      // either add or update the entry
      RefDataTypeCodex myCodex = null;
      for (RefDataTypeCodex rdtCodex : codexList) {
        if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_TYPE) {
          myCodex = rdtCodex;
          break;
        } 
      }
      if (myCodex == null) {
        myCodex = new RefDataTypeCodex();
        myCodex.setAgencyId(agency.getId());
        myCodex.setRefDataTypeId(RefDataType.PROPERTY_TYPE);
        codexList.add(myCodex);
      }
      myCodex.setCodexLabel(propTypeCodexLabel);
    } else {
      // try to delete from the set
      RefDataTypeCodex myCodex = null;
      for (RefDataTypeCodex rdtCodex : codexList) {
        if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_TYPE) {
          myCodex = rdtCodex;
          break;
        } 
      }
      if (myCodex != null) {
        codexList.remove(myCodex);
        dorsService.delete(myCodex);
      }
    }
    if (usePropSubtypeCodex) {
      // either add or update the entry
      RefDataTypeCodex myCodex = null;
      for (RefDataTypeCodex rdtCodex : codexList) {
        if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_SUBTYPE) {
          myCodex = rdtCodex;
          break;
        } 
      }
      if (myCodex == null) {
        myCodex = new RefDataTypeCodex();
        myCodex.setAgencyId(agency.getId());
        myCodex.setRefDataTypeId(RefDataType.PROPERTY_SUBTYPE);
        codexList.add(myCodex);
      }
      myCodex.setCodexLabel(propSubtypeCodexLabel);
    } else {
      // try to delete from the set
      RefDataTypeCodex myCodex = null;
      for (RefDataTypeCodex rdtCodex : codexList) {
        if (rdtCodex.getRefDataTypeId() == RefDataType.PROPERTY_SUBTYPE) {
          myCodex = rdtCodex;
          break;
        } 
      }
      if (myCodex != null) {
        codexList.remove(myCodex);
        dorsService.delete(myCodex);
      }
    }
    dorsService.saveOrUpdateAll(codexList);

    // if "Report visibility (to reviewer) based on incident type" flag is changed:
    // 1) changed from off to on: each account can see all the incident types
    // 2) changed from on to off: for each revewing account delete its entries from user_inctype table
    if (DorsUtil.isTrue(agency.getRptVisibleByInctype()) && DorsUtil.isFalse(savedRptVisibleByInctypeFlag)) {
      // changed from off to on: make changes so that each account can see all the incident types
      List incTypeList = cacheService.getActiveIncidentTypes(agency.getId());
      Set<IncidentTypeMin> incTypeSet = new HashSet<IncidentTypeMin>();
      for (int i=0; i<incTypeList.size(); i++) {
        IncidentType incType = (IncidentType)incTypeList.get(i);
        IncidentTypeMin itm = new IncidentTypeMin();
        itm.setId(incType.getId());
        incTypeSet.add(itm);
      }
      List userList = dorsService.getUserList(agency.getId());
      for (int i=0; i<userList.size(); i++) {
        User user = (User)userList.get(i);
        if (user.getRole().contains(DorsConst.ROLE_RVWING)) {
          user.getAllowedIncidentTypes().clear();
          user.getAllowedIncidentTypes().addAll(incTypeSet);
        }
      }
      dorsService.saveOrUpdateAll(userList);
    } else if (DorsUtil.isFalse(agency.getRptVisibleByInctype()) && DorsUtil.isTrue(savedRptVisibleByInctypeFlag)) {
      // ideally we should do this: for each reviewing account delete its entries from user_inctype table
    }

    boolean rptnoByDateReported = DorsUtil.isTrue(agency.getRptnoByDateReported());

    // if Next Report Number needs to be reset
    if (nextRptNo != null) {
      ReportNumberUtil.resetNextRptNo(agency.getState(), agency.getId(),
          nextRptNo, rptnoByDateReported, agency.getRptNumFormat());
    }

    // if Next CadCall Number needs to be reset
    if (nextCadNo != null) {
      ReportNumberUtil.resetNextCadNo(agency.getState(), agency.getId(),
          nextCadNo, rptnoByDateReported, agency.getCadNumFormat());
    }

    // if Next Seq3 Number needs to be reset
    if (nextSeq3No != null) {
      ReportNumberUtil.resetNextSeq3No(agency.getState(), agency.getId(), nextSeq3No, rptnoByDateReported, agency.getSeq3NumFormat());
    }

    String supportedLanguages = agency.getSupportedAddtlLanguage();
    String enabledLanguages = agency.getEnabledAddtlLanguage();

    // if selectXxxFromList is checked,
    // we need to make sure the default options will be used if no options have been set.
    // Do not do the same thing for Prop Brand/Prop Model/Alarm Company since we don't have the 
    // the list ourselves yet.
    if (DorsUtil.isTrue(agency.getSelectVehMakeFromList())) {
      Collection myVehMakes = dorsService.getRefDataItemList(agency.getId(), RefDataType.VEHICLE_MAKE);
      if (XUtil.isEmpty(myVehMakes)) {
        Collection vehMakes = cacheService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.VEHICLE_MAKE);
        List list = new ArrayList();
        Iterator it = vehMakes.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.VEHICLE_MAKE);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            destItem.setDescX1(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            destItem.setDescX2(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            destItem.setDescX3(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            destItem.setDescX4(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            destItem.setDescX5(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            destItem.setDescX6(srcItem.getDescEn());
          }
          destItem.setParentCode(srcItem.getParentCode());
          destItem.setCode(srcItem.getCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.VEHICLE_MAKE);
      }
    }
    if (DorsUtil.isTrue(agency.getSelectVehModelFromList())) {
      Collection myVehModels = dorsService.getRefDataItemList(agency.getId(), RefDataType.VEHICLE_MODEL);
      if (XUtil.isEmpty(myVehModels)) {
        Collection vehModels = cacheService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.VEHICLE_MODEL);
        List list = new ArrayList();
        Iterator it = vehModels.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.VEHICLE_MODEL);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            destItem.setDescX1(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            destItem.setDescX2(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            destItem.setDescX3(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            destItem.setDescX4(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            destItem.setDescX5(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            destItem.setDescX6(srcItem.getDescEn());
          }
          destItem.setParentCode(srcItem.getParentCode());
          destItem.setCode(srcItem.getCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.VEHICLE_MODEL);
      }
    }
    if (DorsUtil.isTrue(agency.getSelectGunModelFromList())) {
      Collection myGunModels = dorsService.getRefDataItemList(agency.getId(), RefDataType.FIREARM_MODEL);
      if (XUtil.isEmpty(myGunModels)) {
        Collection gunModels = dorsService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.FIREARM_MODEL);
        List list = new ArrayList();
        Iterator it = gunModels.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.FIREARM_MODEL);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            destItem.setDescX1(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            destItem.setDescX2(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            destItem.setDescX3(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            destItem.setDescX4(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            destItem.setDescX5(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            destItem.setDescX6(srcItem.getDescEn());
          }
          destItem.setParentCode(srcItem.getParentCode());
          destItem.setCode(srcItem.getCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.FIREARM_MODEL);
      }
    }
    if (DorsUtil.isTrue(agency.getSelectGunLengthFromList())) {
      Collection myGunLengths = dorsService.getRefDataItemList(agency.getId(), RefDataType.FIREARM_LENGTH);
      if (XUtil.isEmpty(myGunLengths)) {
        Collection gunLengths = cacheService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.FIREARM_LENGTH);
        List list = new ArrayList();
        Iterator it = gunLengths.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.FIREARM_LENGTH);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            destItem.setDescX1(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            destItem.setDescX2(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            destItem.setDescX3(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            destItem.setDescX4(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            destItem.setDescX5(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            destItem.setDescX6(srcItem.getDescEn());
          }
          destItem.setCode(srcItem.getCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.FIREARM_LENGTH);
      }
    }
    if (DorsUtil.isTrue(agency.getSelectGunShotsFromList())) {
      Collection myGunShots = dorsService.getRefDataItemList(agency.getId(), RefDataType.FIREARM_SHOTS);
      if (XUtil.isEmpty(myGunShots)) {
        Collection gunShots = cacheService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.FIREARM_SHOTS);
        List list = new ArrayList();
        Iterator it = gunShots.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.FIREARM_SHOTS);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            destItem.setDescX1(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            destItem.setDescX2(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            destItem.setDescX3(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            destItem.setDescX4(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            destItem.setDescX5(srcItem.getDescEn());
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            destItem.setDescX6(srcItem.getDescEn());
          }
          destItem.setCode(srcItem.getCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.FIREARM_SHOTS);
      }
    }
    if (DorsUtil.isTrue(agency.getSelectPropSubtypeFromList())) {
      Collection myPropSubtypes = dorsService.getRefDataItemList(agency.getId(), RefDataType.PROPERTY_SUBTYPE);
      if (XUtil.isEmpty(myPropSubtypes)) {
        Collection propSubtypes = cacheService.getRefDataItemList(DorsConst.REF_AGENCY_ID, RefDataType.PROPERTY_SUBTYPE);
        List list = new ArrayList();
        Iterator it = propSubtypes.iterator();
        while (it.hasNext()) {
          RefDataItem srcItem = (RefDataItem)it.next();
          RefDataItem destItem = new RefDataItem();
          destItem.setAgencyId(agency.getId());
          destItem.setRefDataTypeId(RefDataType.PROPERTY_SUBTYPE);
          destItem.setDescEn(srcItem.getDescEn());
          if (LocaleUtil.isLanguageEnabled(1, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(1, supportedLanguages);
            destItem.setDescX1(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          if (LocaleUtil.isLanguageEnabled(2, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(2, supportedLanguages);
            destItem.setDescX2(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          if (LocaleUtil.isLanguageEnabled(3, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(3, supportedLanguages);
            destItem.setDescX3(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          if (LocaleUtil.isLanguageEnabled(4, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(4, supportedLanguages);
            destItem.setDescX4(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          if (LocaleUtil.isLanguageEnabled(5, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(5, supportedLanguages);
            destItem.setDescX5(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          if (LocaleUtil.isLanguageEnabled(6, supportedLanguages, enabledLanguages)) {
            String locale = LocaleUtil.getLocaleName(6, supportedLanguages);
            destItem.setDescX6(translateText(RefDataType.PROPERTY_SUBTYPE, srcItem.getDescEn(), locale));
          }
          destItem.setCode(srcItem.getCode());
          destItem.setCodex(srcItem.getCodex());
          destItem.setParentCode(srcItem.getParentCode());
          destItem.setDisplayOrder(srcItem.getDisplayOrder());
          list.add(destItem);
        }
        dorsService.saveOrUpdateAll(list);
        cacheService.flushRefDataItemList(agency.getId(), RefDataType.PROPERTY_SUBTYPE);
      }
    }

    // admin may've turned on these flags and chosen some fields to use,
    // later on when admin turns off the flag, we need to remove these incident type fields.
    boolean flushInctypes = false;
    StringBuffer sbIncTypeIds = new StringBuffer();
    for (IncidentType incType: cacheService.getAllIncidentTypes(agency.getId())) {
      sbIncTypeIds.append(",").append(incType.getId());
    }
    String incTypeIds = sbIncTypeIds.toString().substring(1);
    if (DorsUtil.isTrue(savedUseBicycleFields) && DorsUtil.isFalse(agency.getUseBicycleFields())) {
      int numDeleted = dorsService.deleteBicycleFields(incTypeIds);
      if (numDeleted > 0) {
        flushInctypes = true;
      }
    }
    if (DorsUtil.isTrue(savedUseJewelryFields) && DorsUtil.isFalse(agency.getUseJewelryFields())) {
      if (dorsService.deleteJewelryFields(incTypeIds) > 0) {
        flushInctypes = true;
      }
    }
    if (DorsUtil.isTrue(savedUseBoatFields) && DorsUtil.isFalse(agency.getUseBoatFields())) {
      if (dorsService.deleteBoatFields(incTypeIds) > 0) {
        flushInctypes = true;
      }
    }
    if (DorsUtil.isTrue(savedUseSecurityFields) && DorsUtil.isFalse(agency.getUseSecurityFields())) {
      if (dorsService.deleteSecurityFields(incTypeIds) > 0) {
        flushInctypes = true;
      }
    }
    if (DorsUtil.isTrue(savedUseAlcoholFields) && DorsUtil.isFalse(agency.getUseAlcoholFields())) {
      if (dorsService.deleteAlcoholFields(incTypeIds) > 0) {
        flushInctypes = true;
      }
    }
    if (flushInctypes) {
      cacheService.flushIncidentTypes(agency.getId());
    }

    String remoteIp = DorsUtil.getRemoteIp(requestGlobals.getHTTPServletRequest());
    AdminTrailUtil.recordAdminTrailForAgencyInfo(visit, cacheService, agency, remoteIp, AdminTrailUtil.UPD);

    // now go ahead to save...
    dorsService.saveOrUpdate(agency);

    // Flush the data from cache (for fresh load next time)
    cacheService.flushAgency(agency.getId(), agency.getRefCode());   

    // Go to Confirmation page
    return UpdConfirm.class;
  }


  private String translateText(int refDataTypeId, String label, String locale) {
    return dorsService.translateText(refDataTypeId, label, locale);
  }

  /**
   * Used to remove an existing schedule from database.
   */

  public String getRptSaleServer() {
    String serverHostname = DorsUtil.getMessage("rptSaleApp.server.hostname");
    if (XUtil.isEmpty(serverHostname) || serverHostname.startsWith("<%")) {
      return "";
    } else {
      return new StringBuilder(" [").append(serverHostname).append("]").toString();
    }
  }
  
  public FieldValidator getPhoneValidator(int phoneType) {// 0-inputPhone, 1-inputFax
  	StringBuffer sb = new StringBuffer();
  	if (visit.isAusSouthAustraliaCSPs() && phoneType == 0) { //special handling for AusSouthAustraliaCSPs
  		sb.append(DorsConst.RE_AU_MOBILE_PHONE);
  	} else if (visit.isAgencyInAU()) {
    	sb.append(DorsConst.RE_AU_LANDLINE_PHONE);
    } else {
    	sb.append(DorsConst.RE_US_PHONE);
    }
    if (phoneType == 0) { //for inputPhone
    	sb.append(",required");
    	return fvSource.createValidators(inputPhone, sb.toString());
    } else {
    	return fvSource.createValidators(inputFax, sb.toString());
    }
  }
  
  public String getPhoneLabel() {
  	if (visit.isAusSouthAustraliaCSPs()) { //uses AU toll free number as agency phone
  		return messages.get("phoneAUMobile");
  	} else if (visit.isAgencyInAU()) {
  		return messages.get("phoneAULandline");
  	} else {
  		return messages.get("phone");
  	}
  }
  
  public String getFaxLabel() {
  	if (visit.isAgencyInAU()) {
  		return messages.get("faxAU");
  	} else {
  		return messages.get("fax");
  	}
  }
  
  public int getPhoneType() { // for use in JavaScript
  	if (visit.isAusSouthAustraliaCSPs()) { 
  		return 1;
  	} 
  	return 0;
  }

  public boolean getRedactForAllEmails() {
    return DorsUtil.getRedactForAllEmails(agency.getRefCode(), agency.getRedactForAllEmails());
  }

}
