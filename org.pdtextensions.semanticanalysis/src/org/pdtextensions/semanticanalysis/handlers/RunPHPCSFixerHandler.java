/*******************************************************************************
 * Copyright (c) 2012 The PDT Extension Group (https://github.com/pdt-eg)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.pdtextensions.semanticanalysis.handlers;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.pdtextensions.core.launch.DefaultExecutableLauncher;
import org.pdtextensions.core.launch.ILaunchResponseHandler;
import org.pdtextensions.core.launch.IPHPLauncher;
import org.pdtextensions.core.log.Logger;
import org.pdtextensions.core.ui.PEXUIPlugin;
import org.pdtextensions.core.ui.preferences.PreferenceConstants;
import org.pdtextensions.core.util.ArrayUtil;
import org.pdtextensions.semanticanalysis.PEXAnalysisPlugin;
import org.pdtextensions.semanticanalysis.preferences.PEXPreferenceNames;


public class RunPHPCSFixerHandler extends AbstractHandler {

	private List<String> fixerArgs;
	private IPHPLauncher launcher = new DefaultExecutableLauncher();
	private String fixerPath;
	
	final public static URL CS_FIXER = Platform.getBundle(PEXAnalysisPlugin.PLUGIN_ID).getEntry("Resources/phpcsfixer/phpcsfixer.phar");
	

	@Override
	@SuppressWarnings("unchecked")
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IPreferenceStore store = PEXUIPlugin.getDefault().getPreferenceStore();
		
		fixerPath = store.getString(PreferenceConstants.PREF_PHPCS_PHAR_LOCATION);
		
		if (fixerPath == null || fixerPath.length() == 0) {
			try {
				fixerPath = getDefaultPhar();
			} catch (Exception e) {
				Logger.debug("Unable to get default phar");
				return null;
			}
		}
		
		String defaultFixers = store.getString(PreferenceConstants.PREF_PHPCS_USE_DEFAULT_FIXERS);
		String config = store.getString(PreferenceConstants.PREF_PHPCS_CONFIG);
		
		fixerArgs = new ArrayList<String>();
		
		fixerArgs.add("fix");
		fixerArgs.add("file");
		
		if (!PreferenceConstants.PREF_PHPCS_CONFIG_DEFAULT.equals(config)) {
			fixerArgs.add(" --config=" + config.replace("phpcs_config_", ""));
		}
		
		if ("false".equals(defaultFixers)) {
			
			List<String> fixers = new ArrayList<String>();
			
			for (String key : PEXPreferenceNames.getPHPCSFixerOptions()) {
				
				String option = store.getString(key);
				if (option != null && option.length() > 0) {
					
					String optionValue = getOption(option);
					
					if (optionValue != null) {
						fixers.add(getOption(option));
					}
				}
			}
			
			fixerArgs.add(" --fixers=" + ArrayUtil.implode(",", (String[]) fixers.toArray(new String[fixers.size()])));
		}
		
		List<String> arguments = new ArrayList<String>();
		arguments.add("fix");
		
		ISelection selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		
		if (selection != null && selection instanceof IStructuredSelection) {
			
			IStructuredSelection strucSelection = (IStructuredSelection) selection;
			
			for (Iterator<Object> iterator = strucSelection.iterator(); iterator.hasNext();) {
				
				try {
					Object element = iterator.next();
					if (element instanceof IScriptFolder) {
						IScriptFolder folder = (IScriptFolder) element;
						processFolder(folder);
					} else if (element instanceof ISourceModule) {
						runFixer((ISourceModule) element);
					}
				} catch (Exception e) {
					Logger.logException(e);
				}
			}
		}
		
		return null;
	}
	
	protected void processFolder(IScriptFolder folder) throws IOException, InterruptedException, CoreException {
		
		try {
			for (IModelElement child : folder.getChildren()) {
				if (child instanceof IScriptFolder) {
					processFolder((IScriptFolder) child);
				} else if (child instanceof ISourceModule) {
					runFixer((ISourceModule) child);
				}
			}
		} catch (ModelException e) {
			Logger.logException(e);
		}
	}

	private void runFixer(ISourceModule source) throws IOException, InterruptedException, CoreException {
		
		IResource resource = source.getUnderlyingResource();
		String fileToFix =  resource.getLocation().toOSString();
		fixerArgs.set(1, fileToFix);
		
		Logger.debug("Running cs-fixer: " + fixerPath + " => " + fixerArgs.get(1));
		launcher.launch(fixerPath, fixerArgs.toArray(new String[fixerArgs.size()]), new ILaunchResponseHandler() {
			
			@Override
			public void handle(String response) {
				Logger.debug(response);
			}
		} );
		
		source.getUnderlyingResource().refreshLocal(IResource.DEPTH_ZERO, null);
		
	}
	
	
	protected String getOption(String packed) {
		
		String[] unpacked = packed.split("::");
		
		if (unpacked != null && unpacked.length == 4 && "true".equals(unpacked[3])) {
			return unpacked[1];
		}
		
		return null;
	}
	
	protected String getDefaultPhar() throws Exception
	{
		final URL resolve=FileLocator.resolve(CS_FIXER);

		if (resolve == null) {
			throw new Exception("Unable to load php-cs-fixer.phar");
		}

		IPath path = new Path(resolve.getFile());
		return path.toOSString();
	}	
}
