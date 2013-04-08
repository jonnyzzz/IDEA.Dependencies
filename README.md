IDEA.Dependencies Releases
==========================

For plugin sources, please check `master` branch

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
