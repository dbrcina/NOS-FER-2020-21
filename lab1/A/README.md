# Radovi na cesti
### Zadatak

Stari most je uski most i stoga postavlja ograničenja na promet. Na njemu istovremeno smije biti najviše 3 automobila koji voze u istom smjeru. Simulirati automobile procesom *__Auto__* koji obavlja niže navedene radnje. Napisati program koji stvara *N* automobila, gdje je *N* proizvoljan broj između 5 i 100 koji se određuje prilikom pokretanja programa te svakom automobilu dodjeljuje registarsku oznaku. Smjer se automobilu određuje nasumično.

Proces *__Semafor__* određuje koji automobili će prijeći most, a početni smjer prijelaza se određuje nasumično te se zatim izmjenjuju. Prijelazak mosta se omogućuje kada se zabilježi 3 zahtjeva za prijelaz u trenutnom smjeru ili prođe *X* milisekundi, gdje je *X* slučajan broj između 500 i 1000. Prijelaz mosta traje *Y* milisekundi gdje je *Y* broj između 1000 i 3000.

Procesi međusobno komuniciraju uz pomoć __reda poruka__ koristeći __raspodijeljeni centralizirani protokol__, gdje je proces *__Semafor__* odgovoran za međusobno isključivanje.

    Proces Auto(registarska_oznaka, smjer) {
        // smjer = 0 ili 1
        // registarska oznaka je redni broj automobila u 
        spavaj Z milisekundi; // Z je slučajan broj između 100 i 
        pošalji zahtjev za prijelaz mosta i ispiši("Automobil registarska_oznaka čeka na prelazak preko 
        po primitku poruke "Prijeđi" ispiši("Automobil registarska_oznaka se popeo na 
        po primitku poruke "Prešao" ispiši("Automobil registarska_oznaka je prešao most.");
    }
    
Napomene:
- Obavezno komentirati izvorni tekst programa (programski kod).
- Sve što u zadatku nije zadano, riješiti na proizvoljan način.
