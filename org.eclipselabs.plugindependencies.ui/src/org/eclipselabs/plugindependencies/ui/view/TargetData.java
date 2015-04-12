/*******************************************************************************
 * Copyright (c) 2015 Andrey Loskutov <loskutov@gmx.de>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipselabs.plugindependencies.ui.view;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipselabs.plugindependencies.ui.Activator;

/**
 * @author andrey
 */
public class TargetData {
    private final IFile file;

    /**
     *
     */
    public TargetData(IFile file) {
        super();
        this.file = file;
    }

    public List<String> getPaths(){
        IPath path = file.getLocation();
        if(path == null){
            return Collections.emptyList();
        }
        try {
            List<String> lines = Files.readAllLines(path.toFile().toPath());
            return lines;
        } catch (IOException e) {
            IStatus err = Activator.getDefault().errorStatus("Failed to read file: " + file, e);
            StatusManager.getManager().handle(err, StatusManager.LOG | StatusManager.SHOW);
            return Collections.emptyList();
        }
    }
}
