# Kanji Dojo Data

Data and scripts used by the [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) application

Characters and expressions are located in ```data/``` folder

Scripts to manipulate data are in the ```src``` directory, under ```task``` package.
They can be executed using IntelliJ IDEA or by running a command with required task name

Tasks:

* ```./gradlew run -Ptask=ExportCharactersJson```
* ```./gradlew run -Ptask=ExportDatabase```
* ```./gradlew run -Ptask=ExportExpressionsJson```
* ```./gradlew run -Ptask=ValidateJson```
* ```./gradlew run -Ptask=ExportVariantsInfo```
* ```./gradlew run -Ptask=ApplyExpressionRanks```

If you wish to run scripts you'll need to download necessary data. Scripts and links for downloading
can be found in ```parser_data/``` directory

Download tasks:

* ```./gradlew downloadRadkFile```
* ```./gradlew downloadLeedsFrequencies```
* ```./gradlew downloadjmdictFuriganaJson```
* TODO - replace all other bash scripts with gradle tasks

### Data Sources and Credits

* **KanjiVG**</br>
  Provides writing strokes, radicals information </br>
  License: Creative Commons Attribution-Share Alike 3.0</br>
  Link: https://kanjivg.tagaini.net/
* **Kanji Dic**</br>
  Provides characters info, such as meanings, readings and classifications </br>
  License: Creative Commons Attribution-Share Alike 3.0</br>
  Link: http://www.edrdg.org/wiki/index.php/KANJIDIC_Project
* **JMDict**</br>
  Japanese-Multilingual dictionary, provides expressions </br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://www.edrdg.org/jmdict/j_jmdict.html
* **RADKFILE**
  Provides a decomposition of kanji into radicals to support software which provides a lookup
  service using kanji components
  License: Creative Commons Attribution-Share Alike 4.0
  Link: https://www.edrdg.org/krad/kradinf.html
* **JmdictFurigana**</br>
  Open-source furigana resource to complement the EDICT/Jmdict and ENAMDICT/Jmnedict dictionary
  files </br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://github.com/Doublevil/JmdictFurigana
* **Frequency list by Leeds university**</br>
  Words ranking by frequency of usage in internet </br>
  License: Creative Commons BY</br>
  Link: http://corpus.leeds.ac.uk/list.html