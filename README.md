# icon-evol
Iconicity and Evolution (joint project Tübingen/Lund)

This repository contains the code and the supplementary materials for the following article: **Johannes Dellert, Niklas Erben Johansson, Johan Frid, Gerd Carling (2021): *Preferred sound groups of vocal iconicity reflect evolutionary mechanisms of sound stability and first language acquisition: evidence from Eurasia*. In: Antonio Benítez-Burraco and Ljiljana Progovac (eds.): *Reconstructing prehistoric languages*, theme issue of Philosophical Transactions of the Royal Society B.**

Computation of sound stability (SSt) scores
===
This part is implemented in Java, packaged as a Maven project in the `code` directory.

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
* (optional) Reinfer the sound similarity model by running the script `de.tuebingen.sfs.iconevol.CorrespondenceModelPreparation`.  
This will use the NorthEuraLex version and the sound group definitions placed under `src/main/resources`, and stores the sound similarity model under `src/main/resources/northeuralex-0.9/global-iw-lund.corr`. If you do not make any changes to the input data or the sound groups, this step should not be necessary.
* Running the script `de.tuebingen.sfs.iconevol.SoundGroupStabilityOutput` will print a table in tab-separated format, containing the four stability scores for each sound group:
  ```
  SoundGroup	WeightedNumAlignments	Stable	ShiftInGroup	ShiftOutOfGroup	LossOrGain
  affricates	14609.4501714283	0.265268878888375	0.166925476571934	0.533822612745801	0.033983031793899
  alveolars	279853.229082243	0.590252538805548	0.200773200502448	0.132410172537081	0.076564088154914
  back vowels	129400.7020951	0.25937386931043	0.244037404607773	0.31031540036753	0.186273325714276
  central vowels	39386.6267265968	0.133748530457657	0.030702917163692	0.635551012622713	0.199997539755954
  continuants	136693.278433994	0.461460312916741	0.180703207621992	0.236920566538294	0.120915912922973
  front vowels	217485.196404693	0.309743074570944	0.249008472426277	0.22674580904239	0.214502643960405
  ...
  ```  

Supplementary materials
===
* [S1](https://github.com/jdellert/icon-evol/blob/master/supplements/S1-iconevol-mathematical-supplement.pdf): mathematical description of the sound stability score
* [S2](https://github.com/jdellert/icon-evol/blob/master/supplements/S2-sound-group-definitions.tsv): sound group definitions (as used by the code in order to make stability data compatible with the iconicity data set)
* [S3](https://github.com/jdellert/icon-evol/blob/master/supplements/S3-sound-acquisition-data.tsv): sound acquisition data (separating sound groups by early vs. late acquisition, with references)
* [S4](https://github.com/jdellert/icon-evol/blob/master/supplements/S4-iconevol-combined-dataset.csv): combined dataset (in CSV format) that serves as input to the R script (integrates both datasets)
* [S5](https://github.com/jdellert/icon-evol/blob/master/supplements/S5-script-for-analyses.R): commented R script implementing the statistical analyses described in the article
* [S6](https://github.com/jdellert/icon-evol/blob/master/supplements/S6-result-graphs.zip): ZIP file containing all the result graphs mentioned in the article
