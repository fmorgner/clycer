Clycer
======

Clycer is an implementation of the ORBS algorithm, as first presented by Binkley
et al. [1], designed for the `Cevlop C++ IDE <https://www.cevelop.com>`_.

Installing
----------

Clycer can be installed using the `Update site <http://www.felixmorgner.com/clycer/update>`_
(http://www.felixmorgner.com/clycer/update). Alternatively, you can build Clycer
from source, as described below.

Building
--------

Clycer can either be built using the Eclipse PDE or Maven 3.

Eclipse PDE
~~~~~~~~~~~

Install Eclipse PDE and simply import the projects contained in this repository.
Afterwards, set the Target Platform Definition contained in the project
`com.felixmorgner.clycer.target` as the active TPD. You can then start Clycer by
right-clicking on the `com.felixmorgner.clycer` project and selecting
`Run as -> Eclipse Application`.

Maven
~~~~~

Install Maven 3 and run `mvn package` in the root of this repository. This will
generate a P2 updatesite in `com.felixmorgner.clycer.updatesite/target/site`.
You can then use this updatesite to install Clycer in Cevelop.
