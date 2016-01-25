// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.wizards.exportjob.scriptsmanager;

import java.util.EnumMap;
import java.util.Map;

import org.talend.core.language.ECodeLanguage;
import org.talend.core.language.LanguageManager;
import org.talend.core.repository.constants.FileConstants;
import org.talend.repository.ui.wizards.exportjob.JavaJobScriptsExportWSWizardPage.JobExportType;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.esb.JobJavaScriptESBManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.esb.JobJavaScriptOSGIForESBManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.esb.OSGIJavaScriptForESBWithMavenManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.petals.PetalsJobJavaScriptsManager;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 */
public class JobScriptsManagerFactory {

    public static JobScriptsManager createManagerInstance(Map<ExportChoice, Object> exportChoiceMap, String contextName,
            String launcher, int statisticPort, int tracePort, JobExportType jobExportType) {
        ECodeLanguage language = LanguageManager.getCurrentLanguage();
        if (language == ECodeLanguage.JAVA) {
            switch (jobExportType) {
            case POJO:
                return new JobJavaScriptsManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort);
            case WSWAR:
                return new JobJavaScriptsWSManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort,
                        FileConstants.WAR_FILE_SUFFIX);
            case WSZIP:
                return new JobJavaScriptsWSManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort,
                        FileConstants.ZIP_FILE_SUFFIX);
            case JBOSSESB:
                return new JobJavaScriptESBManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort);
            case PETALSESB:
                return new PetalsJobJavaScriptsManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort);
            case OSGI:
                if (exportChoiceMap.containsKey(ExportChoice.needMavenScript)
                        && exportChoiceMap.get(ExportChoice.needMavenScript) == Boolean.TRUE) {
                    return new OSGIJavaScriptForESBWithMavenManager(exportChoiceMap, contextName, launcher, statisticPort,
                            tracePort);
                } else {
                    return new JobJavaScriptOSGIForESBManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort);
                }
            }
        }
        return new JobJavaScriptsManager(exportChoiceMap, contextName, launcher, statisticPort, tracePort);
    }

    public static Map<ExportChoice, Object> getDefaultExportChoiceMap() {
        Map<ExportChoice, Object> exportChoiceMap = new EnumMap<ExportChoice, Object>(ExportChoice.class);
        exportChoiceMap.put(ExportChoice.needLauncher, true);
        exportChoiceMap.put(ExportChoice.needSystemRoutine, true);
        exportChoiceMap.put(ExportChoice.needUserRoutine, true);
        exportChoiceMap.put(ExportChoice.needTalendLibraries, true);
        exportChoiceMap.put(ExportChoice.needJobItem, true);
        exportChoiceMap.put(ExportChoice.needJobScript, true);
        exportChoiceMap.put(ExportChoice.needContext, true);
        exportChoiceMap.put(ExportChoice.needSourceCode, true);
        exportChoiceMap.put(ExportChoice.applyToChildren, false);
        exportChoiceMap.put(ExportChoice.doNotCompileCode, false);
        exportChoiceMap.put(ExportChoice.needDependencies, true);
        return exportChoiceMap;
    }

}
