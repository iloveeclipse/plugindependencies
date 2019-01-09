# plugindependencies
Project provides both command line and Eclipse UI for static package, plugin and feature dependencies analysis (the dependencies are read from MANIFEST.MF and feature.xml files).

Update site url: https://raw.githubusercontent.com/iloveeclipse/plugindependencies/master/update_site/

The project is started as student work from Oliver Broesamle.

The main goals for the command line:
  * Fast & lightweight
  * No 3rd party dependencies
  * Can be used during build & deploy process
  * Can analyse package & feature dependencies
  
The main goals for UI:
  * Fast & lightweight
  * Useful for analysis of huge target platforms
  * Easy navigation 
  * Easy discovery of platform issues

The plugindependencies plugin contributes "Plug-In Explorer" view to Eclipse.

The plugin offers currently two ways to analyse the "target platforms": via the "Load Target Definition File" button or via the drop down list of available PDE target platforms (both in the "Plug-In Explorer" view).

The custom target files should have suffix `.t2` and located in the workspace. The syntax is trivial and expect every line in the `.t2` file be a path to a directory with plugins/features or entire installation.

The UI always takes currently opened workspace projects into account and adds them to the target platform.

After analysis is complete, one can navigate through the dependency graph and see who includes or requires whom, and which possible missing dependencies were found.

The command line can be started via

`java -jar org.eclipselabs.plugindependencies.core_1.0.5.201703221614.jar`

and offers some analysis functionality (via text reporting) but also possibility to generate two different dependency files (which can be used by other tools computing build dependencies).

See https://github.com/iloveeclipse/plugindependencies/wiki for some more help how to use the plugin.
