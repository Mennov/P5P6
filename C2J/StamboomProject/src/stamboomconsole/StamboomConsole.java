package stamboomconsole;

import stamboomdomain.Geslacht;
import stamboomdomain.Persoon;
import stamboomdomain.Administratie;
import stamboomdomain.Gezin;
import java.util.*;
import stamboomutil.StringUtilities;
import stamboomcontroller.StamboomController;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StamboomConsole {

    // **********datavelden**********************************************
    private final Scanner input;
    private final StamboomController controller;

    // **********constructoren*******************************************
    public StamboomConsole(StamboomController controller) {
        input = new Scanner(System.in);
        this.controller = controller;
    }

    // ***********methoden***********************************************
    public void startMenu() {
        MenuItem choice = kiesMenuItem();
        while (choice != MenuItem.EXIT) {
            switch (choice) {
                case NEW_PERS:
                    invoerNieuwePersoon();
                    break;
                case NEW_ONGEHUWD_GEZIN:
                    invoerNieuwGezin();
                    break;
                case NEW_HUWELIJK:
                    invoerHuwelijk();
                    break;
                case SCHEIDING:
                    invoerScheiding();
                    break;
                case SHOW_PERS:
                    toonPersoonsgegevens();
                    break;
                case SHOW_GEZIN:
                    toonGezinsgegevens();
                    break;
                case LOAD_ADMINISTRATIE:
                    loadAdministratie();
                    break;
                case SAVE_ADMINISTRATIE:
                    saveAdministratie();
                    break;
            }
            choice = kiesMenuItem();
        }
    }

    Administratie getAdmin() {
        return controller.getAdministratie();
    }

    void invoerNieuwePersoon() {
        Geslacht geslacht = null;
        while (geslacht == null) {
            String g = readString("wat is het geslacht (m/v)");
            if (g.toLowerCase().charAt(0) == 'm') {
                geslacht = Geslacht.MAN;
            }
            if (g.toLowerCase().charAt(0) == 'v') {
                geslacht = Geslacht.VROUW;
            }
        }

        String[] vnamen;
        vnamen = readString("voornamen gescheiden door spatie").split(" ");
        
        String tvoegsel;
        tvoegsel = readString("tussenvoegsel");

        String anaam;
        anaam = readString("achternaam");

        Calendar gebdat;
        gebdat = readDate("geboortedatum");

        String gebplaats;
        gebplaats = readString("geboorteplaats");

        Gezin ouders;
        toonGezinnen();
        String gezinsString = readString("gezinsnummer van ouderlijk gezin");
        if (gezinsString.equals("")) {
            ouders = null;
            getAdmin().addPersoon(geslacht, vnamen, anaam, tvoegsel, gebdat, gebplaats, ouders);
        } else {
            ouders = getAdmin().getGezin(Integer.parseInt(gezinsString));
            ouders.breidUitMet(getAdmin().addPersoon(geslacht, vnamen, anaam, tvoegsel, gebdat, gebplaats, ouders));
        }
    }

    void invoerNieuwGezin() {
        System.out.println("wie is de eerste partner?");
        Persoon partner1 = selecteerPersoon();
        if (partner1 == null) {
            System.out.println("onjuiste invoer eerste partner");
            return;
        }
        System.out.println("wie is de tweede partner?");
        Persoon partner2 = selecteerPersoon();
        Gezin gezin = getAdmin().addOngehuwdGezin(partner1, partner2);
        if (gezin == null) {
            System.out.println("gezin is niet geaccepteerd");
        }
    }

    void invoerHuwelijk() {
        System.out.println("wie is de eerste partner?");
        Persoon partner1 = selecteerPersoon();
        if (partner1 == null) {
            System.out.println("onjuiste invoer eerste partner");
            return;
        }
        System.out.println("wie is de tweede partner?");
        Persoon partner2 = selecteerPersoon();
        if (partner2 == null) {
            System.out.println("onjuiste invoer tweede partner");
            return;
        }
        Calendar datum = readDate("datum van huwelijk");
        Gezin g = getAdmin().addHuwelijk(partner1, partner2, datum);
        if (g == null) {
            System.out.println("huwelijk niet voltrokken");
        }
    }

    void invoerScheiding() {
        selecteerGezin();
        int gezinsNr = readInt("kies gezinsnummer");
        input.nextLine();
        Calendar datum = readDate("datum van scheiding");
        Gezin g = getAdmin().getGezin(gezinsNr);
        if (g != null) {
            boolean gelukt = getAdmin().setScheiding(g, datum);
            if (!gelukt) {
                System.out.println("scheiding niet geaccepteerd");
            }
        } else {
            System.out.println("gezin onbekend");
        }
    }

    Persoon selecteerPersoon() {
        String naam = readString("wat is de achternaam");
        List<Persoon> personen = getAdmin().getPersonenMetAchternaam(naam);
        for (Persoon p : personen) {
            System.out.println(p.getNr() + "\t" + p.getNaam() + " " + datumString(p.getGebDat()));
        }
        int invoer = readInt("selecteer persoonsnummer");
        input.nextLine();
        Persoon p = getAdmin().getPersoon(invoer);
        return p;
    }

    Gezin selecteerGezin() {
        String naam = readString("gezin van persoon met welke achternaam");
        List<Persoon> kandidaten = getAdmin().getPersonenMetAchternaam(naam);
        for (Persoon p : kandidaten) {
            List<Gezin> gezinnen = p.getAlsOuderBetrokkenIn();
            System.out.print(p.getNr() + "\t" + p.getNaam() + " " + datumString(p.getGebDat()));
            System.out.print(" gezinnen: ");
            for (Gezin gezin : gezinnen) {
                System.out.print(" " + gezin.getNr());
            }
            System.out.println();
        }
        int invoer = readInt("selecteer gezinsnummer");
        input.nextLine();
        return getAdmin().getGezin(invoer);
    }

    MenuItem kiesMenuItem() {
        System.out.println();
        for (MenuItem m : MenuItem.values()) {
            System.out.println(m.ordinal() + "\t" + m.getOmschr());
        }
        System.out.println();
        int maxNr = MenuItem.values().length - 1;
        int nr = readInt("maak een keuze uit 0 t/m " + maxNr);
        while (nr < 0 || nr > maxNr) {
            nr = readInt("maak een keuze uit 0 t/m " + maxNr);
        }
        input.nextLine();
        return MenuItem.values()[nr];
    }

    void toonPersoonsgegevens() {
        Persoon p = selecteerPersoon();
        if (p == null) {
            System.out.println("persoon onbekend");
        } else {
            System.out.println(p.beschrijving());
        }
    }

    void toonGezinsgegevens() {
        Gezin g = selecteerGezin();
        if (g == null) {
            System.out.println("gezin onbekend");
        } else {
            System.out.println(g.beschrijving());
        }
    }

    void loadAdministratie() {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.substring(0, path.length() - 4);
        File file = new File(path, readString("Bestandslocatie"));
        try {
            controller.deserialize(file);
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(ex.getMessage());
        }
    }

    void saveAdministratie() {
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        path = path.substring(0, path.length() - 4);
        File file = new File(path, readString("Bestandslocatie"));
        try {
            controller.serialize(file);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    void toonGezinnen() {
        int nr = 0;
        Gezin r = getAdmin().getGezin(nr);
        while (r != null) {
            System.out.println(r.toString());
            nr++;
            r = getAdmin().getGezin(nr);
        }
    }

    static void printSpaties(int n) {
        System.out.print(StringUtilities.spaties(n));
    }

    Calendar readDate(String helptekst) {
        String datumString = readString(helptekst + "; voer datum in (dd-mm-jjjj)");
        try {
            return StringUtilities.datum(datumString);
        } catch (IllegalArgumentException exc) {
            System.out.println(exc.getMessage());
            return readDate(helptekst);
        }
    }

    int readInt(String helptekst) {
        boolean invoerOk = false;
        int invoer = -1;
        while (!invoerOk) {
            try {
                System.out.print(helptekst + " ");
                invoer = input.nextInt();
                invoerOk = true;
            } catch (InputMismatchException exc) {
                System.out.println("Let op, invoer moet een getal zijn!");
                input.nextLine();
            }

        }
        return invoer;
    }

    String readString(String helptekst) {
        System.out.print(helptekst + " ");
        String invoer = input.nextLine();
        return invoer;
    }

    String datumString(Calendar datum) {
        return StringUtilities.datumString(datum);
    }

    private String toPascalCase(String s) {
        return s.substring(0, 1).toUpperCase()
                + s.substring(1).toLowerCase();
    }

    public static void main(String[] arg) {
        StamboomController controller = new StamboomController();

        StamboomConsole console = new StamboomConsole(controller);
        console.startMenu();
    }
}
