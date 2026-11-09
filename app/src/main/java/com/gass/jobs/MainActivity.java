package com.gass.jobs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(22, 35, 52);
    private final int muted = Color.rgb(104, 119, 135);
    private final int green = Color.rgb(0, 145, 105);
    private final int canvas = Color.rgb(246, 248, 250);
    private LinearLayout root;
    private LinearLayout content;
    private boolean employerMode = false;
    private EditText searchInput;
    private Spinner governorateFilter;
    private Spinner sectorFilter;
    private Spinner contractFilter;
    private LinearLayout resultsContainer;
    private final ArrayList<Runnable> screenHistory = new ArrayList<Runnable>();
    private Runnable currentScreenAction;
    private Runnable pendingScreenAction;
    private boolean pendingRootScreen;
    private boolean restoringScreen;
    private static final int PICK_PROFILE_PHOTO = 7101;
    private static final int PICK_PROFILE_CV = 7102;
    private ImageView profilePhotoView;
    private TextView profileCvLabel;

    private static class Job {
        final String title, company, location, governorate, sector, contract, description;
        Job(String title, String company, String location, String governorate, String sector, String contract, String description) {
            this.title = title;
            this.company = company;
            this.location = location;
            this.governorate = governorate;
            this.sector = sector;
            this.contract = contract;
            this.description = description;
        }
        String key() { return title + "|" + company + "|" + location; }
    }

    // Fictional examples used only to demonstrate the search UI.
    private final List<Job> demoJobs = new ArrayList<Job>() {{
        add(new Job("Agent commercial", "Entreprise de démonstration", "Kasserine", "Kasserine", "Commerce", "Temps plein",
                "Relation client, prospection et suivi commercial. Expérience souhaitée."));
        add(new Job("Technicien informatique", "Entreprise de démonstration", "Sbeitla", "Kasserine", "Informatique", "CDI",
                "Assistance informatique, maintenance et support aux utilisateurs."));
        add(new Job("Ouvrier agricole", "Exploitation de démonstration", "Fériana", "Kasserine", "Agriculture", "Saisonnier",
                "Travaux agricoles. Débutants acceptés, formation sur place."));
        add(new Job("Électricien bâtiment", "Société fictive", "Kairouan", "Kairouan", "Bâtiment", "CDD",
                "Installation électrique et maintenance sur chantiers. Exemple fictif."));
        add(new Job("Aide-soignant", "Centre de démonstration", "Sfax", "Sfax", "Santé", "Temps plein",
                "Accompagnement des patients et aide aux soins. Exemple fictif."));
        add(new Job("Chauffeur-livreur", "Entreprise fictive", "Tunis", "Tunis", "Transport", "CDI",
                "Livraison locale et suivi des commandes. Exemple fictif."));
    }};

    private static final String MINISTRY_DIRECTORY =
            "https://www.emploi.gov.tn/fr/93/directions-regionales-de-formation-professionnelle-et-de-lemploi";
    private static final String OPEN_DATA_DIRECTORY =
            "https://catalog.data.gov.tn/fr/dataset/reseau-de-bureaux-de-l-emploi-et-du-travail-independant-en-tunisie";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (getSharedPreferences("tun_jobs", MODE_PRIVATE).getBoolean("welcome_done", false)) showHome();
        else showWelcome();
    }

    private void prepareScreen(boolean rootScreen, Runnable action) {
        pendingRootScreen = rootScreen;
        pendingScreenAction = action;
    }

    @Override public void onBackPressed() {
        if (!screenHistory.isEmpty()) {
            Runnable previous = screenHistory.remove(screenHistory.size() - 1);
            restoringScreen = true;
            previous.run();
            restoringScreen = false;
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Quitter TUN-JOBS ?")
                .setMessage("Voulez-vous vraiment fermer l'application ?")
                .setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Quitter", (dialog, which) -> finish())
                .show();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, height));
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setMinHeight(76);
        b.setMinimumHeight(76);
        b.setMaxLines(3);
        b.setSingleLine(false);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(primary ? Color.WHITE : ink);
        b.setBackground(shape(primary ? green : Color.rgb(232, 238, 242), 24));
        b.setPadding(14, 8, 14, 8);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le lien.", Toast.LENGTH_SHORT).show();
        }
    }

    private void base(String title, String subtitle) {
        if (pendingRootScreen) {
            screenHistory.clear();
        } else if (!restoringScreen && currentScreenAction != null) {
            screenHistory.add(currentScreenAction);
        }
        if (pendingScreenAction != null) currentScreenAction = pendingScreenAction;
        pendingScreenAction = null;
        pendingRootScreen = false;

        root = column();
        root.setBackgroundColor(canvas);
        setContentView(root);

        LinearLayout header = column();
        header.setPadding(22, 16, 22, 18);
        header.setBackgroundColor(Color.WHITE);
        LinearLayout brandRow = row();
        Button menuButton = button("⋮", false, this::showMenu);
        menuButton.setTextSize(30);
        menuButton.setContentDescription("Open navigation menu");
        menuButton.setMinHeight(72);
        menuButton.setMinimumHeight(72);
        brandRow.addView(menuButton, new LinearLayout.LayoutParams(80, 72));
        TextView brand = text("TUN-JOBS", 17, green, true);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, 48, 1);
        brandParams.leftMargin = 10;
        brandRow.addView(brand, brandParams);
        header.addView(brandRow);
        space(header, 8);
        header.addView(text(title, 25, ink, true));
        header.addView(text(subtitle, 14, muted, false));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = column();
        content.setPadding(18, 18, 18, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = row();
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(5, 8, 5, 8);
        nav.addView(button("Emplois", false, this::showHome), new LinearLayout.LayoutParams(0, 48, 1));
        nav.addView(button("Sources", false, this::showOfficialSources), new LinearLayout.LayoutParams(0, 48, 1));
        nav.addView(button("Bureaux", false, this::showEmploymentOffices), new LinearLayout.LayoutParams(0, 48, 1));
        nav.addView(button("Profil", false, this::showProfile), new LinearLayout.LayoutParams(0, 48, 1));
        root.addView(nav);
    }

    private void card(Job job) {
        LinearLayout c = column();
        c.setPadding(17, 15, 17, 15);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(job.title, 18, ink, true));
        space(c, 5);
        c.addView(text(job.company + "  •  " + job.location + " (" + job.governorate + ")", 13, muted, false));
        space(c, 10);
        c.addView(text(job.sector + " • " + job.contract, 12, green, true));
        space(c, 8);
        c.addView(text(job.description, 14, ink, false));
        space(c, 12);
        LinearLayout actions = row();
        actions.addView(button("Voir l'offre", true, () -> showJob(job.title, job.company, job.location, job.description)),
                new LinearLayout.LayoutParams(0, 56, 1));
        Set<String> saved = new HashSet<String>(getSharedPreferences("gass_jobs_saved", MODE_PRIVATE)
                .getStringSet("keys", new HashSet<String>()));
        boolean isSaved = saved.contains(job.key());
        actions.addView(button(isSaved ? "Enregistré ✓" : "Enregistrer", false, () -> toggleSaved(job)),
                new LinearLayout.LayoutParams(0, 56, 1));
        c.addView(actions);
        content.addView(c);
        space(content, 12);
    }

    private void toggleSaved(Job job) {
        android.content.SharedPreferences prefs = getSharedPreferences("gass_jobs_saved", MODE_PRIVATE);
        Set<String> saved = new HashSet<String>(prefs.getStringSet("keys", new HashSet<String>()));
        if (saved.contains(job.key())) saved.remove(job.key());
        else saved.add(job.key());
        prefs.edit().putStringSet("keys", saved).apply();
        if (resultsContainer != null) renderJobs();
        Toast.makeText(this, saved.contains(job.key()) ? "Offre enregistrée." : "Offre retirée des favoris.", Toast.LENGTH_SHORT).show();
    }

    private void infoCard(String title, String detail, String actionLabel, Runnable action) {
        LinearLayout c = column();
        c.setPadding(17, 15, 17, 15);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(title, 18, ink, true));
        space(c, 8);
        c.addView(text(detail, 14, muted, false));
        space(c, 12);
        c.addView(button(actionLabel, true, action));
        content.addView(c);
        space(content, 12);
    }

    private Spinner filterSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private String selected(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "Tous" : value.toString();
    }

    private void renderJobs() {
        if (resultsContainer == null) return;
        resultsContainer.removeAllViews();
        LinearLayout previousContent = content;
        content = resultsContainer;
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String governorate = selected(governorateFilter);
        String sector = selected(sectorFilter);
        String contract = selected(contractFilter);

        content.addView(text("Résultats de démonstration", 20, ink, true));
        space(content, 5);
        content.addView(text("Ces exemples sont fictifs : aucune candidature réelle n'est possible pour le moment.", 12, muted, false));
        space(content, 12);

        int count = 0;
        for (Job job : demoJobs) {
            String searchable = (job.title + " " + job.company + " " + job.location + " " + job.governorate + " "
                    + job.sector + " " + job.contract + " " + job.description).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (!"Tous".equals(governorate) && !job.governorate.equalsIgnoreCase(governorate)) continue;
            if (!"Tous".equals(sector) && !job.sector.equalsIgnoreCase(sector)) continue;
            if (!"Tous".equals(contract) && !job.contract.equalsIgnoreCase(contract)) continue;
            card(job);
            count++;
        }
        if (count == 0) {
            content.addView(text("Aucune offre ne correspond à ces filtres.", 15, muted, false));
        } else {
            content.addView(text(count + " résultat(s)", 12, muted, false));
        }
        content = previousContent;
    }

    private void showHome() {
        prepareScreen(true, this::showHome);
        base("Trouvez votre prochain emploi", "Les opportunités près de chez vous, en Tunisie.");
        LinearLayout switcher = row();
        switcher.addView(button("Chercher un emploi", !employerMode, () -> { employerMode = false; showHome(); }),
                new LinearLayout.LayoutParams(0, 48, 1));
        switcher.addView(button("Recruter", employerMode, () -> { employerMode = true; showHome(); }),
                new LinearLayout.LayoutParams(0, 48, 1));
        content.addView(switcher);
        space(content, 12);
        content.addView(button("Trouver un bureau de l'emploi", false, this::showEmploymentOffices));
        space(content, 8);
        content.addView(button("Consulter les sources officielles d'emploi", false, this::showOfficialSources));
        space(content, 18);

        if (employerMode) {
            content.addView(text("Vous recrutez ?", 21, ink, true));
            space(content, 8);
            content.addView(text("Préparez une annonce pour les candidats de Kasserine et de toute la Tunisie. Votre brouillon reste uniquement sur cet appareil : il n'est pas publié ni transmis.", 14, muted, false));
            space(content, 14);
            content.addView(button("Créer une offre", true, this::showEmployerDraft));
            space(content, 10);
            content.addView(button("Voir les sources officielles", false, () -> openExternal(MINISTRY_DIRECTORY)));
            return;
        }

        content.addView(text("Rechercher une offre", 20, ink, true));
        space(content, 8);
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15);
        searchInput.setHint("Métier, mot-clé ou entreprise");
        searchInput.setPadding(14, 10, 14, 10);
        searchInput.setBackground(shape(Color.WHITE, 14));
        content.addView(searchInput, new LinearLayout.LayoutParams(-1, 52));
        space(content, 10);

        content.addView(text("Gouvernorat", 13, muted, true));
        governorateFilter = filterSpinner(new String[] {
                "Tous", "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana", "Kairouan",
                "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax", "Gafsa", "Tozeur",
                "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        content.addView(governorateFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 6);

        content.addView(text("Secteur", 13, muted, true));
        sectorFilter = filterSpinner(new String[] {
                "Tous", "Agriculture", "Bâtiment", "Commerce", "Informatique", "Santé", "Transport"
        });
        content.addView(sectorFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 6);

        content.addView(text("Type de contrat", 13, muted, true));
        contractFilter = filterSpinner(new String[] {
                "Tous", "CDI", "CDD", "Temps plein", "Saisonnier"
        });
        content.addView(contractFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 12);

        LinearLayout filterActions = row();
        filterActions.addView(button("Rechercher", true, this::renderJobs),
                new LinearLayout.LayoutParams(0, 48, 1));
        filterActions.addView(button("Effacer", false, () -> {
            searchInput.setText("");
            governorateFilter.setSelection(0);
            sectorFilter.setSelection(0);
            contractFilter.setSelection(0);
            renderJobs();
        }), new LinearLayout.LayoutParams(0, 48, 1));
        content.addView(filterActions);
        space(content, 18);
        resultsContainer = column();
        content.addView(resultsContainer);
        renderJobs();
    }

    private void showMenu() {
        prepareScreen(false, this::showMenu);
        base("Menu TUN-JOBS", "Accès rapide à votre espace et aux réglages.");
        LinearLayout banner = column();
        banner.setPadding(20, 22, 20, 22);
        banner.setBackground(shape(Color.rgb(22, 35, 52), 18));
        banner.addView(text("TUN-JOBS", 25, Color.WHITE, true));
        space(banner, 6);
        banner.addView(text("EMPLOI • RECRUTEMENT • TUNISIE", 13, Color.rgb(180, 218, 205), true));
        space(banner, 8);
        banner.addView(text("GASS-OS  •  BY AYMEN GHARSALLI", 13, Color.WHITE, false));
        content.addView(banner);
        space(content, 18);
        content.addView(button("⌂   Emplois", false, this::showHome), new LinearLayout.LayoutParams(-1, 58));
        space(content, 8);
        content.addView(button("♙   Mon profil", false, this::showProfile), new LinearLayout.LayoutParams(-1, 58));
        space(content, 8);
        content.addView(button("⚙   Paramètres", false, this::showSettings), new LinearLayout.LayoutParams(-1, 58));
        space(content, 8);
        content.addView(button("ⓘ   À propos", false, this::showAboutMe), new LinearLayout.LayoutParams(-1, 58));
        space(content, 8);
        content.addView(button("Sites officiels de l'emploi", false, this::showOfficialSources), new LinearLayout.LayoutParams(-1, 58));
        space(content, 8);
        content.addView(button("Bureaux régionaux", false, this::showEmploymentOffices), new LinearLayout.LayoutParams(-1, 58));
    }

    private void showSettings() {
        prepareScreen(false, this::showSettings);
        base("Paramètres", "Préférences de TUN-JOBS sur cet appareil.");
        android.content.SharedPreferences prefs = getSharedPreferences("tun_jobs", MODE_PRIVATE);
        content.addView(text("Langue de l'application", 20, ink, true));
        space(content, 8);
        content.addView(text("La traduction complète en arabe, français et anglais sera ajoutée progressivement. Pour le moment, l'interface principale reste en français.", 15, muted, false));
        space(content, 16);
        content.addView(text("Confidentialité", 18, ink, true));
        space(content, 6);
        content.addView(text("Votre profil, votre photo, votre CV et les brouillons d'offres sont enregistrés localement. Cette version ne possède pas encore de compte en ligne ni de synchronisation serveur.", 14, muted, false));
        space(content, 16);
        content.addView(button("Réafficher l'écran de bienvenue au prochain lancement", false, () -> {
            prefs.edit().putBoolean("welcome_done", false).apply();
            Toast.makeText(this, "L'écran de bienvenue sera affiché au prochain lancement.", Toast.LENGTH_LONG).show();
        }), new LinearLayout.LayoutParams(-1, 58));
        space(content, 10);
        content.addView(button("Retour aux emplois", true, this::showHome), new LinearLayout.LayoutParams(-1, 58));
    }

    private void showAboutMe() {
        prepareScreen(false, this::showAboutMe);
        base("À propos", "Qui sommes-nous ?");
        LinearLayout banner = column();
        banner.setPadding(20, 22, 20, 22);
        banner.setBackground(shape(Color.rgb(22, 35, 52), 18));
        banner.addView(text("TUN-JOBS", 28, Color.WHITE, true));
        space(banner, 8);
        banner.addView(text("Une plateforme tunisienne pour rapprocher les candidats et les employeurs.", 16, Color.WHITE, false));
        content.addView(banner);
        space(content, 18);
        content.addView(text("Créé par GASS-OS", 20, green, true));
        space(content, 6);
        content.addView(text("Conçu par Aymen Gharsalli. Notre objectif est de faciliter l'accès aux opportunités d'emploi à Kasserine et dans les 24 gouvernorats de Tunisie.", 15, muted, false));
        space(content, 16);
        content.addView(text("Version de démonstration", 17, ink, true));
        space(content, 6);
        content.addView(text("Les annonces visibles actuellement sont des exemples fictifs. Les portails publics s'ouvrent directement sur leurs sites officiels. Les comptes, candidatures et publications en ligne nécessitent encore un service sécurisé.", 14, muted, false));
        space(content, 18);
        content.addView(button("Retour", true, this::showMenu), new LinearLayout.LayoutParams(-1, 58));
    }

    private void showOfficialSources() {
        prepareScreen(true, this::showOfficialSources);
        base("Offres officielles en Tunisie", "Accès direct aux portails publics — sans copie ni republication des annonces.");
        content.addView(text("Consultez les annonces directement sur les sites officiels. Les disponibilités, conditions et dates limites sont celles publiées par chaque organisme.", 14, muted, false));
        space(content, 16);
        infoCard("ANETI — Agence nationale pour l'emploi", "Portail national de l'emploi et des services aux demandeurs d'emploi et aux entreprises. Les offres doivent être vérifiées sur le site source.", "Ouvrir emploi.nat.tn", () -> openExternal("https://www.emploi.nat.tn/"));
        infoCard("Concours de la fonction publique", "Portail national des concours et recrutements publics. Vérifiez les conditions, pièces exigées et délais dans chaque avis.", "Ouvrir concours.gov.tn", () -> openExternal("https://www.concours.gov.tn/"));
        infoCard("Ministère de l'Emploi", "Informations officielles, directions régionales, programmes et actualités du ministère.", "Ouvrir le ministère", () -> openExternal("https://www.emploi.gov.tn/fr/"));
        infoCard("Ministère de l'Intérieur — recrutements", "Portail de recrutement et avis publiés par le ministère. Consultez chaque annonce pour les modalités officielles.", "Ouvrir le portail de recrutement", () -> openExternal("https://recrutementdaf.interieur.gov.tn/Daaf/home"));
        space(content, 6);
        content.addView(text("Transparence et fiabilité", 18, ink, true));
        space(content, 6);
        content.addView(text("GASS-JOBS ne prétend pas disposer d'une API ANETI ni d'un flux RSS public vérifié. Pour éviter les annonces périmées ou non autorisées, cette version ouvre les portails d'origine. Une intégration automatique sera ajoutée uniquement après confirmation des conditions de réutilisation ou accord du fournisseur.", 13, muted, false));
        space(content, 10);
        content.addView(button("Voir les bureaux régionaux", true, this::showEmploymentOffices));
    }

    private void officeCard(String governorate, String director, String address, String phone, String email) {
        LinearLayout c = column();
        c.setPadding(18, 18, 18, 18);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(governorate, 19, ink, true));
        space(c, 7);
        c.addView(text("Responsable : " + director, 15, green, true));
        space(c, 8);
        c.addView(text(address, 15, muted, false));
        c.addView(text("Téléphone : " + phone, 15, muted, false));
        if (email != null && !email.isEmpty()) {
            c.addView(text("E-mail : " + email, 15, muted, false));
        }
        space(c, 14);
        LinearLayout actions = row();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(button("Appeler", true, () -> openExternal("tel:" + phone.replace(" ", ""))),
                new LinearLayout.LayoutParams(0, 58, 1));
        if (email != null && !email.isEmpty()) {
            actions.addView(button("E-mail", false, () -> openExternal("mailto:" + email)),
                    new LinearLayout.LayoutParams(0, 58, 1));
        }
        c.addView(actions);
        content.addView(c);
        space(content, 16);
    }

    private void showEmploymentOffices() {
        prepareScreen(true, this::showEmploymentOffices);
        base("Emploi dans les régions", "Directions régionales — les 24 gouvernorats.");
        content.addView(text("KASSERINE EN PREMIER", 13, green, true));
        space(content, 6);
        content.addView(text("Contacts régionaux officiels", 20, ink, true));
        space(content, 8);
        content.addView(text("Coordonnées publiées par le Ministère de l'Emploi et de la Formation professionnelle. Ces fiches concernent les directions régionales, pas les adresses individuelles de chaque bureau local. Page officielle consultée le 8 novembre 2026.", 13, muted, false));
        space(content, 14);

        String[][] regions = {
            {"Kasserine", "Mohamed Armi", "Rue 7 novembre, BP 111, 1200 Kasserine", "77 475 366", "Mohamed.Armi@mfpe.state.tn"},
            {"Tunis", "Sondes Gmir", "6, rue de Cologne, 1001 Tunis", "71 789 389", "sondes.gmir@mfpe.state.tn"},
            {"Ariana", "Faten Jerjini", "17 Bis, rue Ahmed Khabtheni, 2080 Ariana", "71 717 696", "faten.jerjini@mfpe.state.tn"},
            {"Ben Arous", "Adel Khelifi", "56, rue de la France, 2013 Ben Arous", "71 381 510", "adel.khelifi@mfpe.state.tn"},
            {"Manouba", "Mbarka Rgoubi", "5, rue Abou Taieb Almoutanebbi, 2010 Manouba", "70 604 133", "mbarka.ragoubi@mfpe.state.tn"},
            {"Nabeul", "Adel Zitouni", "33, rue de France, 8000 Nabeul", "72 285 913", "adel.zitouni@mfpe.state.tn"},
            {"Zaghouan", "Anis Lachheb", "Rue Narjess, 1100 Zaghouan", "72 675 588", "anis.lachheb@mfpe.state.tn"},
            {"Bizerte", "Hamouda Gharech", "169, rue Hassen Ennouri, 7000 Bizerte", "72 432 378", "hamouda.gharech@mfpe.state.tn"},
            {"Béja", "Hatem Raihani", "Avenue Habib Bourguiba, 9000 Béja", "78 456 177", "hatem.raihani@mfpe.state.tn"},
            {"Jendouba", "Mohamed Rahali", "9, avenue 9 Avril, 8100 Jendouba", "78 601 160", "mohamed.rahali@mfpe.state.tn"},
            {"Le Kef", "Jalel Twihri", "Rue Taha Hassine, 7120 Le Kef", "78 223 822", "jalel.twihri@mfpe.state.tn"},
            {"Siliana", "Atef Bouraoui", "Rue 31 Août, 6100 Siliana", "78 872 580", "atef.bouraoui@mfpe.state.tn"},
            {"Kairouan", "Abdellatif Abbassi", "16, rue Baiet Elhikma, 3100 Kairouan", "77 230 876", "abdellatif.abbassi@mfpe.state.tn"},
            {"Sidi Bouzid", "Aymen Boualagui", "51, rue Karra Hdid, 9100 Sidi Bouzid", "76 633 545", "aymen.bouallagui@mfpe.state.tn"},
            {"Sousse", "Tarek Mahjoub", "Rue Ksantina, 4003 Sousse", "73 221 600", "tarek.mahjoub@mfpe.state.tn"},
            {"Monastir", "Sawssen Touir", "Rue Ibn Sina, 5000 Monastir", "73 461 866", "sawssen.touir@mfpe.state.tn"},
            {"Mahdia", "Khaled Hadj Hassen", "Rue Bachir Sfar, BP 111, 5100 Mahdia", "73 696 421", ""},
            {"Sfax", "Noura Zidi", "Rue Jemelleddine Afghani, Bouderiar 1, Sfax", "74 222 592", "noura.zidi@mfpe.state.tn"},
            {"Gafsa", "Mohamed Sniha", "Rue Erroman Douali, 2100 Gafsa", "76 228 346", "sniha.mohamed@mfpe.state.tn"},
            {"Tozeur", "Omar Khmira", "Route de Nafta, 2200 Tozeur", "76 468 631", "omar.khmira@mfpe.state.tn"},
            {"Kébili", "Othman Mahmoudi", "Route de Gabès, 4200 Kébili", "75 490 682", "othman.mahmoudi@mfpe.state.tn"},
            {"Gabès", "Nizar Khaldi", "Rue Belkacem Bazmi, 6000 Gabès", "75 270 435", "nizar.khaldi@mfpe.state.tn"},
            {"Médenine", "Mchabet Meftah", "Rue Borma, 4100 Médenine", "75 641 915", "meftah.mchabet@mfpe.state.tn"},
            {"Tataouine", "Kilani Oun", "Avenue El Koufa, cité El Mahrajen, Tataouine", "75 844 994", "kilani.jabri@mfpe.state.tn"}
        };
        for (String[] region : regions) {
            officeCard(region[0], region[1], region[2], region[3], region[4]);
        }

        space(content, 8);
        content.addView(text("Sources et actualisation", 18, ink, true));
        space(content, 6);
        content.addView(text("Source principale : annuaire officiel des directions régionales du ministère. Les coordonnées peuvent changer ; vérifiez-les auprès du ministère avant un déplacement. Pour Kasserine, la page ministérielle indique 3 bureaux d'emploi, mais cette liste ne prétend pas fournir leurs trois adresses individuelles.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir l'annuaire officiel du ministère", true, () -> openExternal(MINISTRY_DIRECTORY)));
        space(content, 16);
        content.addView(text("Jeu de données national complémentaire", 17, ink, true));
        space(content, 5);
        content.addView(text("Le catalogue national propose un fichier CSV et XLSX sous licence CC BY. Attention : les ressources sont datées du 7 mars 2023 et leur couverture temporelle indiquée est 2016–2019. Elles ne sont pas utilisées ici comme source de coordonnées actuelles sans revalidation.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir le jeu de données officiel", false, () -> openExternal(OPEN_DATA_DIRECTORY)));
    }

    private EditText profileField(String hint, String value) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setTextSize(17);
        field.setMinHeight(64);
        field.setHint(hint);
        field.setText(value);
        field.setPadding(16, 12, 16, 12);
        field.setBackground(shape(Color.WHITE, 14));
        return field;
    }

    private void pickProfileFile(int requestCode, String mime) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (requestCode == PICK_PROFILE_CV) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"});
        } else intent.setType(mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (Exception ignored) { }
        android.content.SharedPreferences.Editor edit = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
        if (requestCode == PICK_PROFILE_PHOTO) edit.putString("photo_uri", uri.toString());
        if (requestCode == PICK_PROFILE_CV) edit.putString("cv_uri", uri.toString());
        edit.apply();
        if (requestCode == PICK_PROFILE_PHOTO && profilePhotoView != null) profilePhotoView.setImageURI(uri);
        if (requestCode == PICK_PROFILE_CV && profileCvLabel != null) profileCvLabel.setText("CV ajouté : " + uri.getLastPathSegment());
        Toast.makeText(this, requestCode == PICK_PROFILE_PHOTO ? "Photo sélectionnée." : "CV sélectionné.", Toast.LENGTH_SHORT).show();
    }

    private void showWelcome() {
        prepareScreen(true, this::showWelcome);
        base("Bienvenue sur TUN-JOBS", "L'emploi et le recrutement, partout en Tunisie.");
        space(content, 26);
        content.addView(text("Votre prochain emploi peut commencer ici.", 27, ink, true));
        space(content, 12);
        content.addView(text("Trouvez des opportunités près de chez vous, consultez les sources officielles et préparez votre profil candidat. Les employeurs peuvent aussi préparer leurs offres.", 16, muted, false));
        space(content, 26);
        content.addView(button("Je cherche un emploi", true, () -> {
            getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
            employerMode = false;
            showHome();
        }), new LinearLayout.LayoutParams(-1, 60));
        space(content, 12);
        content.addView(button("Je propose un emploi", false, () -> {
            getSharedPreferences("tun_jobs", MODE_PRIVATE).edit().putBoolean("welcome_done", true).apply();
            employerMode = true;
            showHome();
        }), new LinearLayout.LayoutParams(-1, 60));
        space(content, 22);
        content.addView(text("TUN-JOBS — Une plateforme tunisienne pour rapprocher candidats et employeurs.", 13, muted, false));
    }

    private void showProfile() {
        prepareScreen(true, this::showProfile);
        base("Mon profil", "Préparez vos informations de candidat sur cet appareil.");
        android.content.SharedPreferences p = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE);
        content.addView(text("Informations personnelles", 20, ink, true));
        space(content, 8);
        ImageView photo = new ImageView(this);
        profilePhotoView = photo;
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setBackground(shape(Color.WHITE, 18));
        String photoUri = p.getString("photo_uri", "");
        if (!photoUri.isEmpty()) {
            try { photo.setImageURI(Uri.parse(photoUri)); } catch (Exception ignored) { }
        }
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(112, 112);
        photoParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(photo, photoParams);
        space(content, 8);
        content.addView(button("Ajouter / modifier ma photo", false, () -> pickProfileFile(PICK_PROFILE_PHOTO, "image/*")),
                new LinearLayout.LayoutParams(-1, 56));
        space(content, 14);

        EditText name = profileField("Nom et prénom", p.getString("name", ""));
        EditText phone = profileField("Téléphone", p.getString("phone", ""));
        EditText email = profileField("Adresse e-mail", p.getString("email", ""));
        EditText city = profileField("Ville / gouvernorat", p.getString("city", "Kasserine"));
        EditText experience = profileField("Expérience et métier recherché", p.getString("experience", ""));
        EditText education = profileField("Formation / diplôme", p.getString("education", ""));
        EditText skills = profileField("Compétences", p.getString("skills", ""));
        EditText availability = profileField("Disponibilité", p.getString("availability", ""));
        EditText[] fields = {name, phone, email, city, experience, education, skills, availability};
        String[] labels = {"Nom et prénom *", "Téléphone *", "E-mail", "Ville / gouvernorat", "Expérience / métier recherché", "Formation / diplôme", "Compétences", "Disponibilité"};
        for (int i=0;i<fields.length;i++) {
            content.addView(text(labels[i], 14, muted, true));
            space(content, 4);
            content.addView(fields[i], new LinearLayout.LayoutParams(-1, 58));
            space(content, 10);
        }

        String cvUri = p.getString("cv_uri", "");
        profileCvLabel = text(cvUri.isEmpty() ? "Aucun CV sélectionné" : "CV ajouté : " + Uri.parse(cvUri).getLastPathSegment(), 14, muted, false);
        content.addView(profileCvLabel);
        space(content, 6);
        content.addView(button("Joindre mon CV (PDF ou document)", false, () -> pickProfileFile(PICK_PROFILE_CV, "application/pdf")),
                new LinearLayout.LayoutParams(-1, 56));
        space(content, 14);
        content.addView(button("Enregistrer mon profil sur ce téléphone", true, () -> {
            if (name.getText().toString().trim().isEmpty() || phone.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Le nom et le téléphone sont obligatoires.", Toast.LENGTH_LONG).show();
                return;
            }
            android.content.SharedPreferences.Editor e = getSharedPreferences("tun_jobs_profile", MODE_PRIVATE).edit();
            String[] keys = {"name","phone","email","city","experience","education","skills","availability"};
            for (int i=0;i<fields.length;i++) e.putString(keys[i], fields[i].getText().toString().trim());
            e.putBoolean("profile_saved", true).apply();
            Toast.makeText(this, "Profil enregistré uniquement sur cet appareil.", Toast.LENGTH_LONG).show();
        }), new LinearLayout.LayoutParams(-1, 60));
        space(content, 8);
        content.addView(text("Confidentialité : vos informations, votre photo et votre CV restent sur ce téléphone. Ils ne sont pas envoyés ni publiés. Le partage avec les employeurs sera activé uniquement après mise en place d'un service sécurisé et avec votre accord.", 13, muted, false));

        space(content, 22);
        content.addView(text("Mes offres sauvegardées", 20, ink, true));
        Set<String> saved = new HashSet<String>(getSharedPreferences("gass_jobs_saved", MODE_PRIVATE)
                .getStringSet("keys", new HashSet<String>()));
        int savedCount = 0;
        for (Job job : demoJobs) {
            if (saved.contains(job.key())) {
                infoCard(job.title, job.company + " • " + job.location + " (" + job.governorate + ")", "Voir l'offre", () ->
                        showJob(job.title, job.company, job.location, job.description));
                savedCount++;
            }
        }
        if (savedCount == 0) content.addView(text("Aucune offre sauvegardée pour le moment.", 14, muted, false));
    }

    private void showJob(String title, String company, String location, String description) {
        prepareScreen(false, () -> showJob(title, company, location, description));
        base(title, company + " • " + location);
        content.addView(text(description, 16, ink, false));
        space(content, 18);
        content.addView(button("Postuler (bientôt)", true, this::showNotice));
        space(content, 10);
        content.addView(button("Retour aux offres", false, this::showHome));
    }

    private EditText draftField(String hint, boolean multiline) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setTextSize(17);
        field.setMinHeight(multiline ? 132 : 62);
        field.setSingleLine(!multiline);
        if (multiline) {
            field.setMinLines(4);
            field.setGravity(Gravity.TOP);
        }
        field.setPadding(18, 15, 18, 15);
        field.setBackground(shape(Color.WHITE, 14));
        return field;
    }

    private void showEmployerDraft() {
        prepareScreen(false, this::showEmployerDraft);
        base("Préparer une offre", "Brouillon privé — non publié.");
        content.addView(text("Titre du poste *", 13, muted, true));
        EditText title = draftField("Ex. Technicien de maintenance", false);
        content.addView(title, new LinearLayout.LayoutParams(-1, 52));
        space(content, 10);
        content.addView(text("Entreprise *", 13, muted, true));
        EditText company = draftField("Nom de l'entreprise", false);
        content.addView(company, new LinearLayout.LayoutParams(-1, 52));
        space(content, 10);
        content.addView(text("Gouvernorat *", 13, muted, true));
        Spinner region = filterSpinner(new String[] {
                "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana",
                "Kairouan", "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax",
                "Gafsa", "Tozeur", "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        content.addView(region, new LinearLayout.LayoutParams(-1, 48));
        space(content, 10);
        content.addView(text("Secteur", 13, muted, true));
        Spinner sector = filterSpinner(new String[] {
                "Commerce", "Agriculture", "Bâtiment", "Informatique", "Santé", "Transport", "Autre"
        });
        content.addView(sector, new LinearLayout.LayoutParams(-1, 48));
        space(content, 10);
        content.addView(text("Type de contrat", 13, muted, true));
        Spinner contract = filterSpinner(new String[] {"CDI", "CDD", "Temps plein", "Temps partiel", "Saisonnier", "Stage"});
        content.addView(contract, new LinearLayout.LayoutParams(-1, 48));
        space(content, 10);
        content.addView(text("Salaire (facultatif)", 13, muted, true));
        EditText salary = draftField("Ex. 900–1200 TND / mois", false);
        content.addView(salary, new LinearLayout.LayoutParams(-1, 52));
        space(content, 10);
        content.addView(text("Description et compétences *", 13, muted, true));
        EditText description = draftField("Missions, expérience, compétences, horaires…", true);
        content.addView(description, new LinearLayout.LayoutParams(-1, 116));
        space(content, 14);
        content.addView(text("Ne saisissez pas de données personnelles de candidats ici. Le brouillon est enregistré localement sur ce téléphone. La publication, la vérification de l'employeur et les candidatures seront activées après mise en place d'un backend sécurisé.", 12, muted, false));
        space(content, 14);
        content.addView(button("Enregistrer le brouillon sur ce téléphone", true, () -> {
            String jobTitle = title.getText().toString().trim();
            String employer = company.getText().toString().trim();
            String details = description.getText().toString().trim();
            if (jobTitle.isEmpty() || employer.isEmpty() || details.isEmpty()) {
                Toast.makeText(this, "Complétez les champs obligatoires (*).", Toast.LENGTH_LONG).show();
                return;
            }
            getSharedPreferences("gass_jobs_drafts", MODE_PRIVATE).edit()
                    .putString("title", jobTitle)
                    .putString("company", employer)
                    .putString("governorate", selected(region))
                    .putString("sector", selected(sector))
                    .putString("contract", selected(contract))
                    .putString("salary", salary.getText().toString().trim())
                    .putString("description", details)
                    .putLong("saved_at", System.currentTimeMillis())
                    .apply();
            Toast.makeText(this, "Brouillon enregistré sur cet appareil. Rien n'a été publié.", Toast.LENGTH_LONG).show();
        }));
        space(content, 10);
        content.addView(button("Retour", false, this::showHome));
        android.content.SharedPreferences saved = getSharedPreferences("gass_jobs_drafts", MODE_PRIVATE);
        if (saved.contains("title")) {
            space(content, 18);
            content.addView(text("Dernier brouillon enregistré", 17, ink, true));
            space(content, 5);
            content.addView(text(saved.getString("title", "") + " — " + saved.getString("company", "") + " (" + saved.getString("governorate", "") + ")", 14, muted, false));
            space(content, 8);
            content.addView(button("Charger ce brouillon", false, () -> {
                title.setText(saved.getString("title", ""));
                company.setText(saved.getString("company", ""));
                salary.setText(saved.getString("salary", ""));
                description.setText(saved.getString("description", ""));
                setSpinnerValue(region, saved.getString("governorate", "Kasserine"));
                setSpinnerValue(sector, saved.getString("sector", "Commerce"));
                setSpinnerValue(contract, saved.getString("contract", "CDI"));
            }));
            space(content, 8);
            content.addView(button("Supprimer le brouillon", false, () -> {
                saved.edit().clear().apply();
                Toast.makeText(this, "Brouillon supprimé de cet appareil.", Toast.LENGTH_SHORT).show();
                showEmployerDraft();
            }));
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) spinner.setSelection(position);
    }

    private void showNotice() {
        prepareScreen(false, this::showNotice);
        base("Fonction en préparation", "Cette première version est une base de démonstration.");
        content.addView(text("Les candidatures, comptes et publications seront ajoutés après la mise en place du backend sécurisé.", 15, muted, false));
        space(content, 14);
        content.addView(button("Retour", true, this::showHome));
    }
}
