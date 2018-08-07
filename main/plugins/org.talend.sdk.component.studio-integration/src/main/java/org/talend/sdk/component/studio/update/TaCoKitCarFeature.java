// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.sdk.component.studio.update;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.runtime.service.ITaCoKitService;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.util.TaCoKitConst;
import org.talend.sdk.component.studio.util.TaCoKitUtil;
import org.talend.sdk.component.studio.util.TaCoKitUtil.GAV;
import org.talend.updates.runtime.model.ITaCoKitCarFeature;
import org.talend.updates.runtime.model.UpdateSiteLocationType;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public class TaCoKitCarFeature implements ITaCoKitCarFeature {

    private boolean autoReloadAfterInstalled = true;

    private TaCoKitCar car;

    public TaCoKitCarFeature(TaCoKitCar car) throws Exception {
        this.car = car;
    }

    @Override
    public boolean isInstalled(IProgressMonitor progress) throws Exception {
        boolean isInstalled = false;
        List<GAV> installedGavs = TaCoKitUtil.getInstalledComponents(progress);
        if (installedGavs != null && !installedGavs.isEmpty()) {
            TaCoKitUtil.checkMonitor(progress);
            List<GAV> newComponents = getCar().getComponents();
            if (newComponents != null) {
                // 1. as long as there is one component not be installed, we consider the car is not
                // installed<br/>
                // 2. if there is a newer version component installed, we consider the component is installed.
                List<GAV> installedComponents = new ArrayList<>();
                for (GAV newComponent : newComponents) {
                    TaCoKitUtil.checkMonitor(progress);
                    boolean alreadyInstalled = false;
                    for (GAV installedGav : installedGavs) {
                        TaCoKitUtil.checkMonitor(progress);
                        if (alreadyInstalled) {
                            break;
                        }
                        try {
                            if (StringUtils.equals(installedGav.getGroupId() + ":" + installedGav.getArtifactId(), //$NON-NLS-1$
                                    newComponent.getGroupId() + ":" + newComponent.getArtifactId())) { //$NON-NLS-1$
                                // version format like: 1.0.0-SNAPSHOT
                                String installedVersionString = installedGav.getVersion().split("-")[0]; //$NON-NLS-1$
                                String newVersionString = newComponent.getVersion().split("-")[0]; //$NON-NLS-1$
                                String[] installedVersion = installedVersionString.split("\\."); //$NON-NLS-1$
                                String[] newVersion = newVersionString.split("\\."); //$NON-NLS-1$
                                alreadyInstalled = true;
                                for (int i = 0; i < newVersion.length; ++i) {
                                    if (installedVersion.length - 1 < i) {
                                        alreadyInstalled = false;
                                        break;
                                    }
                                    int iVersion = Integer.valueOf(installedVersion[i]);
                                    int nVersion = Integer.valueOf(newVersion[i]);
                                    if (iVersion < nVersion) {
                                        alreadyInstalled = false;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            alreadyInstalled = true;
                            ExceptionHandler.process(e);
                        }
                    }
                    if (alreadyInstalled) {
                        installedComponents.add(newComponent);
                    }
                }

                if (installedComponents.size() < newComponents.size()) {
                    // means there are new components not been installed yet
                    isInstalled = false;
                } else {
                    isInstalled = true;
                }
            }
        }
        return isInstalled;
    }

    @Override
    public IStatus install(IProgressMonitor progress, List<URI> allRepoUris) throws Exception {
        IStatus status = null;
        try {
            TaCoKitUtil.checkMonitor(progress);
            boolean succeed = install(progress);
            if (succeed) {
                status = new Status(IStatus.OK, TaCoKitConst.BUNDLE_ID,
                        Messages.getString("TaCoKitCarFeature.status.succeed", getName())); //$NON-NLS-1$
                if (!needRestart() && isAutoReloadAfterInstalled()) {
                    // if studio need to restart, then no need to reload here
                    String log = ITaCoKitService.getInstance().reload(progress);
                    ExceptionHandler.log(log);
                }
            } else {
                status = new Status(IStatus.ERROR, TaCoKitConst.BUNDLE_ID,
                        Messages.getString("TaCoKitCarFeature.status.failed", getName())); //$NON-NLS-1$
            }
        } catch (InterruptedException e) {
            status = new Status(IStatus.CANCEL, TaCoKitConst.BUNDLE_ID, Messages.getString("progress.cancel"), e); //$NON-NLS-1$
        } catch (Exception e) {
            status = new Status(IStatus.ERROR, TaCoKitConst.BUNDLE_ID,
                    Messages.getString("TaCoKitCarFeature.status.failed", getName()), e); //$NON-NLS-1$
        }
        return status;
    }

    public boolean install(IProgressMonitor progress) throws Exception {
        TaCoKitCar tckCar = getCar();
        String[] carCmd = new String[] { "java", "-jar", "\"" + tckCar.getCarFile().getAbsolutePath() + "\"", "studio-deploy", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
                "\"" + URIUtil.toFile(Platform.getInstallLocation().getURL().toURI()).getAbsolutePath() + "\"" }; //$NON-NLS-1$ //$NON-NLS-2$
        Process exec = Runtime.getRuntime().exec(carCmd);
        while (exec.isAlive()) {
            Thread.sleep(100);
            TaCoKitUtil.checkMonitor(progress);
        }
        TaCoKitUtil.checkMonitor(progress);
        return exec.exitValue() == 0;
    }

    @Override
    public int compareTo(Object o) {
        TaCoKitCar sTckCar = getCar();
        TaCoKitCar oTckCar = null;
        if (o instanceof TaCoKitCarFeature) {
            oTckCar = ((TaCoKitCarFeature) o).getCar();
        } else if (o instanceof TaCoKitCar) {
            oTckCar = (TaCoKitCar) o;
        } else {
            if (o == null) {
                return 1;
            } else {
                return -1;
            }
        }
        if (oTckCar == null || sTckCar == null) {
            if (oTckCar != null) {
                return -1;
            } else if (sTckCar != null) {
                return 1;
            } else {
                return 0;
            }
        }
        return sTckCar.compareTo(oTckCar);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        return "TaCoKitCarFeature [car=" + this.car.toString() + "]";
    }

    @Override
    public EnumSet<UpdateSiteLocationType> getUpdateSiteCompatibleTypes() {
        return EnumSet.allOf(UpdateSiteLocationType.class);
    }

    @Override
    public boolean mustBeInstalled() {
        return false;
    }

    @Override
    public boolean needRestart() {
        // seems ReloadAction#reload can't reload config.ini?
        return true;
    }

    @Override
    public String getName() {
        return getCar().getName();
    }

    @Override
    public String getDescription() {
        return getCar().getDescription();
    }

    @Override
    public String getVersion() {
        return getCar().getCarVersion();
    }

    @Override
    public boolean isAutoReloadAfterInstalled() {
        return this.autoReloadAfterInstalled;
    }

    @Override
    public void setAutoReloadAfterInstalled(boolean autoReload) {
        this.autoReloadAfterInstalled = autoReload;
    }

    @Override
    public File getCarFile() {
        return getCar().getCarFile();
    }

    private TaCoKitCar getCar() {
        return this.car;
    }

}
