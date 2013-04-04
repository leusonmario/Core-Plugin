package org.pdtextensions.semanticanalysis.preferences.csfixer;

import org.eclipse.php.internal.ui.preferences.util.Key;
import org.pdtextensions.core.PEXCorePlugin;
import org.pdtextensions.core.ui.preferences.launcher.LauncherKeyBag;
import org.pdtextensions.semanticanalysis.preferences.AnalysPreferenceConstants.Keys;

@SuppressWarnings("restriction")
public class FixerKeyBag implements LauncherKeyBag {

	private final Key exeKey = new Key(PEXCorePlugin.PLUGIN_ID, Keys.PHP_EXECUTABLE); 
	private final Key pharKey = new Key(PEXCorePlugin.PLUGIN_ID, Keys.FIXER_PHAR); 
	private final Key useKey = new Key(PEXCorePlugin.PLUGIN_ID, Keys.USE_PROJECT_PHAR); 
	
	@Override
	public Key[] getAllKeys() {
		return new Key[]{exeKey, pharKey, useKey};		
	}

	@Override
	public Key getPHPExecutableKey() {
		return exeKey;
	}

	@Override
	public Key getScriptKey() {
		return pharKey;
	}

	@Override
	public Key getUseProjectKey() {
		return useKey;
	}
}
