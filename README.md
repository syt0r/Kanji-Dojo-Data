# Kanji Dojo Data

Data and scripts used by the [Kanji Dojo](https://github.com/syt0r/Kanji-Dojo) application

Characters and expressions are located in ```data/``` folder

If you wish to run scripts you'll need to download necessary data, scripts and links for downloading can be found in ```parser_data/``` directory

Scripts to manipulate data are in the ```src``` directory, under ```task``` package. 
They can be executed using IntelliJ IDEA or by running a command with required task name, for example:

```./gradlew run -Ptask=ValidateJson```


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
* **JmdictFurigana**</br>
  Open-source furigana resource to complement the EDICT/Jmdict and ENAMDICT/Jmnedict dictionary files </br>
  License: Creative Commons Attribution-Share Alike 4.0</br>
  Link: https://github.com/Doublevil/JmdictFurigana