// Tunn takkåpa för takuttag (DCL-uttag med krok) med kabellås
// -------------------------------------------------------------
// Två delar:
//   1) kapa      – själva kåpan. Öppen uppåt mot taket, platt botten.
//                  Inuti bottnen sitter en nav med invändig gänga + kona.
//   2) kabellas  – en gängad, slitsad "spännhylsa" som skruvas in i navet
//                  underifrån. När den skruvas in pressar konan ihop
//                  fingrarna runt kabeln => kåpan låses på kabeln.
//
// Båda delarna skrivs ut UTAN support:
//   kapa:     botten nedåt på bädden (som en skål)
//   kabellas: greppskivan nedåt på bädden, fingrarna uppåt
//
// Välj del: openscad -D 'part="kapa"' ... / "kabellas" / "allt" / "snitt"

part = "allt";          // "kapa" | "kabellas" | "allt" | "snitt"

/* [Kåpa] */
inner_d   = 120;        // innerdiameter (mm)
depth     = 70;         // invändigt djup från takkant till bottenplatta (mm, min 60)
wall      = 1.8;        // väggtjocklek (mm)
floor_t   = 2.0;        // bottentjocklek (mm)
chamfer   = 5;          // 45°-fas på nedre ytterkanten (utskrivbar utan support)

/* [Kabel] */
cable_d   = 7;          // kabelns diameter (mm). Greppar ca cable_d-1.5 ... cable_d+1
bore      = cable_d + 1;// hål genom kabellåset

/* [Gänga] */
thr_d     = 22;         // gängans ytterdiameter (mm)
pitch     = 3.5;        // stigning (mm)
thr_e     = 0.6;        // excentricitet => gängdjup = 2*thr_e
thr_len   = 6;          // gänglängd (mm)
clear     = 0.35;       // radiellt spel hon-gänga

/* [Spännkona] */
cone_ang  = 25;         // konans halva toppvinkel (grader)
squeeze   = 1.2;        // radiell klämning vid helt iskruvad hylsa (mm)
slots     = 4;          // antal slitsar
slot_w    = 1.4;        // slitsbredd (mm)

/* [Greppskiva] */
head_d    = 36;
head_h    = 6;
ribs      = 18;

$fn = 96;

// ---------- härledda mått ----------
outer_r   = inner_d/2 + wall;
total_h   = depth + floor_t;
k         = 2*tan(cone_ang);          // diameterändring per mm
z_cone0   = thr_len + 1;              // konan börjar här (från bottenytan z=0)
d_cone0   = 15;                       // konans största diameter
d_top     = bore + 0.6;               // hål överst i navet
z_cone1   = z_cone0 + (d_cone0 - d_top)/k;
boss_h    = z_cone1 + 1.2;            // navets höjd från bottenytan
boss_d    = thr_d + 8;

function cone_d(z) = d_cone0 - k*(z - z_cone0);

// ---------- gänga (linear_extrude med twist av excentrisk cirkel) ----------
module thread(d, len, extra = 0) {
    r = d/2 - thr_e + extra;
    linear_extrude(height = len, twist = -360*len/pitch,
                   slices = ceil(len/pitch*48), convexity = 6)
        translate([thr_e, 0]) circle(r = r, $fn = 64);
}

// ---------- kåpa ----------
module shell_profile() {
    // 2D-profil (r,z) som roteras
    difference() {
        polygon([[0,0], [outer_r - chamfer, 0], [outer_r, chamfer],
                 [outer_r, total_h], [0, total_h]]);
        polygon([[0, floor_t], [inner_d/2 - chamfer*0.41, floor_t],
                 [inner_d/2, floor_t + chamfer*0.41],
                 [inner_d/2, total_h + 1], [0, total_h + 1]]);
    }
}

module kapa() {
    difference() {
        union() {
            rotate_extrude($fn = 240) shell_profile();
            // nav med fas upptill
            cylinder(d = boss_d, h = boss_h - 1);
            translate([0,0,boss_h - 1]) cylinder(d1 = boss_d, d2 = boss_d - 2, h = 1);
            // fyra stödribbor mellan nav och botten (styvhet)
            for (a = [0:90:359]) rotate(a)
                translate([boss_d/2 - 1, -0.8, 0]) cube([10, 1.6, floor_t + 3]);
        }
        // invändig gänga
        translate([0,0,-0.01]) thread(thr_d, thr_len + 0.02, clear);
        // avlastning mellan gänga och kona
        translate([0,0,thr_len - 0.01]) cylinder(d = thr_d - 2*thr_e*2 + 2*clear, h = 1.02);
        // kona
        translate([0,0,z_cone0])
            cylinder(d1 = d_cone0, d2 = d_top, h = z_cone1 - z_cone0 + 0.01);
        // kabelhål upptill
        cylinder(d = d_top, h = boss_h + 1);
        // fas i gängans ingång
        translate([0,0,-0.01]) cylinder(d1 = thr_d + 1, d2 = thr_d - 2*thr_e*2, h = 1.2);
    }
}

// ---------- kabellås (spännhylsa) ----------
// Byggs i "monterat" läge: greppskivans översida i z=0, gängan uppåt.
module kabellas_assembled() {
    tip_z = z_cone1 - 0.6;
    difference() {
        union() {
            // greppskiva
            translate([0,0,-head_h]) difference() {
                union() {
                    cylinder(d1 = head_d - 2, d2 = head_d, h = 1);
                    translate([0,0,1]) cylinder(d = head_d, h = head_h - 1);
                }
                for (a = [0:360/ribs:359]) rotate(a)
                    translate([head_d/2 + 0.2, 0, -1]) cylinder(d = 2.4, h = head_h + 2, $fn = 16);
            }
            // gänga
            intersection() {
                thread(thr_d, thr_len);
                // fas i gängans topp
                cylinder(d1 = thr_d + 2, d2 = thr_d - 4, h = thr_len + 0.6);
            }
            // spännfingrar: följer konan, men 'squeeze' mm större radiellt
            translate([0,0,thr_len - 0.01])
                cylinder(d = cone_d(z_cone0) + 2*squeeze - 0.4, h = z_cone0 - thr_len + 0.02);
            translate([0,0,z_cone0])
                cylinder(d1 = cone_d(z_cone0) + 2*squeeze - 0.4,
                         d2 = cone_d(tip_z) + 2*squeeze,
                         h = tip_z - z_cone0);
        }
        // kabelhål med avfasade kanter
        translate([0,0,-head_h - 1]) cylinder(d = bore, h = 100);
        translate([0,0,-head_h - 0.01]) cylinder(d1 = bore + 4, d2 = bore, h = 2);
        // slitsar
        for (a = [0:360/slots:359]) rotate(a + 45)
            translate([0, -slot_w/2, 1]) cube([thr_d, slot_w, 50]);
    }
}

module kabellas() {   // utskriftsläge: greppskivan på bädden
    translate([0,0,head_h]) kabellas_assembled();
}

// ---------- utdata ----------
if (part == "kapa")      kapa();
if (part == "kabellas")  kabellas();
if (part == "allt") {
    kapa();
    translate([outer_r + head_d/2 + 10, 0, 0]) kabellas();
}
if (part == "snitt") {   // monterat, uppskuret för kontroll
    difference() {
        union() {
            color("white") kapa();
            color("orange") rotate(-360*3/pitch*0) kabellas_assembled();
        }
        translate([0, -200, -50]) cube([400, 400, 400]);
    }
}
