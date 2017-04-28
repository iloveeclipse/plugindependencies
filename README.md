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

The command line can be started via

`java -jar org.eclipselabs.plugindependencies.core_1.0.5.201703221614.jar`
