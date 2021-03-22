# Baza podataka
### Zadatak

Pretpostavimo da u sustavu imamo *N* procesa i jednu bazu podataka (u ovom slučaju baza se simulira nizom struktura podataka) koja za svaki proces struktura podataka procesa sadrži identifikator procesa, vrijednost logičkog sata procesa te broj ulazaka u kritični odsječak procesa. Neka je baza podataka dijeljena između procesa na način da je promjena vrijednosti od strane jednog procesa vidljiva svim ostalim procesima. Pristup bazi podataka predstavlja kritični odsječak: najviše jedan proces u svakom trenutku može biti u kritičnom odsječku. Svaki proces 5 puta pristupa bazi podataka (kritičnom odsječku) i prilikom svakog pristupa radi sljedeće:

1. U bazi podataka, ažurira svoju vrijednost logičkog sata trenutnom i inkrementira svoj broj ulazaka u kritični odsječak.
2. Ispiše sadržaj cijele (ne samo svog unosa) baze podataka na standardni izlaz.
3. Spava *X* milisekundi gdje je *X* je slučajan broj između 100 i 2000

Na početku glavni proces stvara N  procesa (broj N se zadaje i može biti u intervalu [3,10]) koji dalje međusobno komuniciraju običnim ili imenovanim __cjevovodima__ (svejedno). Sinkronizirajte pristupanje bazi podataka koristeći

- __Lamportov raspodijeljeni protokol__ (rješavaju studenti čija je __zadnja__ znamenka JMBAG __parna__) ili
- __protokol Ricarta i Agrawala__ (rješavaju studenti čija je __zadnja__ znamenka JMBAG __neparna__).

Napomene:

- Bazu podataka možete definirati kao "struct db_entry database[N]".
- Za dijeljenje baze podataka između procesa koristiti zajednički spremnik (sustavski pozive mmap ili shmat).
- Svi procesi ispisuju poruku koju šalju i poruku koju primaju.
- Obavezno komentirati izvorni tekst programa (programski kod).
- Sve što u zadatku nije zadano, riješiti na proizvoljan način.
