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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.runtime.service.ITaCoKitService;
import org.talend.sdk.component.studio.i18n.Messages;
import org.talend.sdk.component.studio.util.TaCoKitConst;
import org.talend.sdk.component.studio.util.TaCoKitUtil;
import org.talend.sdk.component.studio.util.TaCoKitUtil.GAV;
import org.talend.updates.runtime.feature.model.Category;
import org.talend.updates.runtime.feature.model.Type;
import org.talend.updates.runtime.model.AbstractExtraFeature;
import org.talend.updates.runtime.model.UpdateSiteLocationType;
import org.talend.updates.runtime.model.interfaces.ITaCoKitCarFeature;
import org.talend.updates.runtime.nexus.component.ComponentIndexBean;
import org.talend.updates.runtime.utils.PathUtils;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public class TaCoKitCarFeature extends AbstractExtraFeature implements ITaCoKitCarFeature {

    private boolean autoReloadAfterInstalled = true;

    private TaCoKitCar car;

    private Object carLock = new Object();

    public TaCoKitCarFeature(ComponentIndexBean indexBean) {
        super(indexBean.getBundleId(), indexBean.getName(), indexBean.getVersion(), indexBean.getDescription(),
                indexBean.getMvnURI(), indexBean.getImageMvnURI(), indexBean.getProduct(), indexBean.getCompatibleStudioVersion(),
                null, PathUtils.convert2Types(indexBean.getTypes()), PathUtils.convert2Categories(indexBean.getCategories()),
                Boolean.valueOf(indexBean.getDegradable()), false, false);
    }

    public TaCoKitCarFeature(TaCoKitCar car) throws Exception {
        super(car.toString(), car.getName(), car.getCarVersion(), car.getDescription(), null, null, null, null, null,
                PathUtils.convert2Types(Type.TCOMP.getKeyWord() + "," + Type.TCOMP_V1.getKeyWord()),
                PathUtils.convert2Categories(Category.ALL.getKeyWord()), false, false, false);
        this.car = car;
    }

    @Override
    public boolean isInstalled(IProgressMonitor progress) throws Exception {
        boolean isInstalled = false;
        List<GAV> installedComponents = TaCoKitUtil.getInstalledComponents(progress);
        if (installedComponents != null && !installedComponents.isEmpty()) {
            TaCoKitUtil.checkMonitor(progress);
            List<GAV> newComponents = getCar(progress).getComponents();
            if (newComponents != null) {
                Map<GAV, GAV> alreadyInstalledComponentMap = filterAlreadyInstalledComponent(installedComponents, newComponents,
                        progress);

                if (alreadyInstalledComponentMap.size() < newComponents.size()) {
                    // means there are new components not been installed yet
                    isInstalled = false;
                } else {
                    // TODO
                    if (CommonsPlugin.isHeadless()) {
                        // commandline
                    } else {
                        // GUI
                    }
                    isInstalled = true;
                }
            }
        }
        return isInstalled;
    }

    /**
     * 1. As long as there is one component not be installed, we consider the car is not installed<br/>
     * 2. If there is a newer version component installed, we consider the component is installed.
     * 
     * @return Map<GAV, GAV> newComponent to installedComponent
     */
    private Map<GAV, GAV> filterAlreadyInstalledComponent(List<GAV> installedComponents, List<GAV> newComponents,
            IProgressMonitor progress) throws Exception {
        Map<GAV, GAV> alreadyInstalledComponentMap = new HashMap<>();
        for (GAV newComponent : newComponents) {
            TaCoKitUtil.checkMonitor(progress);
            boolean alreadyInstalled = false;
            GAV curInstalledComponent = null;
            for (GAV installedComponent : installedComponents) {
                TaCoKitUtil.checkMonitor(progress);
                if (alreadyInstalled) {
                    break;
                }
                try {
                    if (StringUtils.equals(
                            installedComponent.getGroupId() + ":" + installedComponent.getArtifactId() + ":" //$NON-NLS-1$ //$NON-NLS-2$
                                    + installedComponent.getClassifier() + ":" + installedComponent.getType(), //$NON-NLS-1$
                            newComponent.getGroupId() + ":" + newComponent.getArtifactId() + ":" //$NON-NLS-1$ //$NON-NLS-2$
                                    + newComponent.getClassifier() + ":" + newComponent.getType())) { //$NON-NLS-1$
                        // version format like: 1.0.0-SNAPSHOT
                        String installedVersionString = installedComponent.getVersion().split("-")[0]; //$NON-NLS-1$
                        String newVersionString = newComponent.getVersion().split("-")[0]; //$NON-NLS-1$
                        String[] installedVersion = installedVersionString.split("\\."); //$NON-NLS-1$
                        String[] newVersion = newVersionString.split("\\."); //$NON-NLS-1$
                        alreadyInstalled = true;
                        for (int i = 0; i < newVersion.length; ++i) {
                            if (installedVersion.length - 1 < i) {
                                alreadyInstalled = false;
                                break;
                            }
                            try {
                                int iVersion = Integer.valueOf(installedVersion[i]);
                                int nVersion = Integer.valueOf(newVersion[i]);
                                if (iVersion < nVersion) {
                                    alreadyInstalled = false;
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                String installedVersionStr = installedVersion[i];
                                String newVersionStr = newVersion[i];
                                int compareResult = 0;
                                if (installedVersionStr == null || newVersionStr == null) {
                                    if (installedVersionStr == null && newVersionStr == null) {
                                        compareResult = 0;
                                    } else {
                                        if (installedVersionStr == null) {
                                            compareResult = -1;
                                        } else {
                                            compareResult = 1;
                                        }
                                    }
                                } else {
                                    compareResult = installedVersionStr.compareTo(newVersionStr);
                                }
                                if (compareResult < 0) {
                                    alreadyInstalled = false;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    alreadyInstalled = true;
                    ExceptionHandler.process(e);
                } finally {
                    if (alreadyInstalled) {
                        curInstalledComponent = installedComponent;
                    }
                }
            }
            if (alreadyInstalled) {
                alreadyInstalledComponentMap.put(newComponent, curInstalledComponent);
            }
        }
        return alreadyInstalledComponentMap;
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

    @SuppressWarnings("nls")
    public boolean install(IProgressMonitor progress) throws Exception {
        TaCoKitCar tckCar = getCar(progress);
        String[] carCmd = new String[] {
                new File(System.getProperty("java.home"),
                        "bin/java" + (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? ".exe" : ""))
                        .getAbsolutePath(),
                "-jar", "\"" + tckCar.getCarFile().getAbsolutePath() + "\"", "studio-deploy",
                "\"" + URIUtil.toFile(Platform.getInstallLocation().getURL().toURI()).getAbsolutePath() + "\"" };
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
        TaCoKitCar sTckCar = null;
        try {
            sTckCar = getCar(new NullProgressMonitor());
        } catch (Exception e) {
            ExceptionHandler.process(e);
        }
        TaCoKitCar oTckCar = null;
        if (o instanceof TaCoKitCarFeature) {
            try {
                oTckCar = ((TaCoKitCarFeature) o).getCar(new NullProgressMonitor());
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
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
        String name = super.getName();
        if (StringUtils.isBlank(name)) {
            try {
                return getCar(new NullProgressMonitor()).getName();
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
            return null;
        } else {
            return name;
        }
    }

    @Override
    public String getDescription() {
        String description = super.getDescription();
        if (StringUtils.isBlank(description)) {
            try {
                return getCar(new NullProgressMonitor()).getDescription();
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
            return null;
        } else {
            return description;
        }
    }

    @Override
    public String getVersion() {
        String version = super.getVersion();
        if (StringUtils.isBlank(version)) {
            try {
                return getCar(new NullProgressMonitor()).getCarVersion();
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }
            return null;
        } else {
            return version;
        }
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
    public File getCarFile(IProgressMonitor progress) throws Exception {
        return getCar(new NullProgressMonitor()).getCarFile();
    }

    private TaCoKitCar getCar(IProgressMonitor progress) throws Exception {
        if (this.car != null) {
            return this.car;
        }
        synchronized (carLock) {
            this.car = new TaCoKitCar(getStorage().getFeatureFile(progress));
        }
        return this.car;
    }

}
