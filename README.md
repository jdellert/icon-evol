# icon-evol
Iconicity and Evolution (joint project Tübingen/Lund)

Computation of sound stability scores
===
* Java code resides in `code` directory

Setting up the environment
---
* clone the repository and recursively update the submodules
  ```
  git clone https://github.com/jdellert/icon-evol.git
  cd icon-evol/code
  git submodule update --init --recursive
  ```
* import as a Maven project into a Java IDE, e.g. IntelliJ or Eclipse (pom.xml in code directory)
* configure the following directories as source roots:
  ```
  src/main/java
  iwsa/src
  iwsa/bin-utils/src/main/java
  iwsa/cldf-java/src/main/java 
  ```  
* build the project

Reproducing the SSt values used in the article
---
* (optional) Reinfer the sound similarity model by running the script `de.tuebingen.sfs.iconevol.CorrespondenceModelPreparation`. This will use the NorthEuraLex version and the sound group definitions placed under `src/main/resources`, and stores the sound similarity model under `src/main/resources/northeuralex-0.9/global-iw-lund.corr`. If you do not make any changes to the input data or the sound groups, this step should not be necessary.

Supplementary materials
===
* S1: mathematical description of the sound stability score
* S2: 
* S3: 
* S4: 
* S5: 
* S6: 
