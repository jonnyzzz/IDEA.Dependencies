IDEA.Dependencies
=================

This is a simple plugin that tries to 
find unused library/module dependencies
in IDEA-platform projects.

The main idea is to resolve all PsiElements within
a module to figure out what modules are reached there. 
All TEST or COMPILE and unreachable modules are
subject for removal.

License
=======
Apache 2.0.
See LICENSE.txt


Plugin Binaries
===============
Checkout `releases` branch in the repo

Plugin is deployed into [Plugins Repository](http://plugins.jetbrains.com/plugin?pr=&pluginId=7222)


Building
========

Call `ant -f fetch.xml fetch` from the `build` directory

Use IntelliJ IDEA for the build/development




This project uses https://github.com/jonnyzzz/intellij-ant-maven
to simplify development


