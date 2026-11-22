// Tunn takkåpa för takuttag (DCL-uttag med krok) – U/V-form med grov plastskruv
// -----------------------------------------------------------------------------
// Delar:
//   1) kapa  – kåpan. Profilen är mitt emellan U och V: raka sidor upptill som
//              mjukt går över i en 45°-avsmalning ner mot en liten platt botten.
//              I botten sitter ett nav med grov invändig gänga + spännkona.
//   2) skruv – ihålig plastskruv med väldigt grov trapetsgänga (stigning 8 mm).
//              Kabeln går genom skruven. När den dras åt pressar konan i navet
//              ihop skruvens slitsade topp runt kabeln => kåpan låses på kabeln.
//              Ca ett halvt varv från kontakt till låst.
//
// Utskrift utan support:
//   kapa:  UPP OCH NER – takkanten på bädden (alla innerytor är >= 45°)
//   skruv: skallen på bädden
//
// Välj del: openscad -D 'part="kapa"' ... / "skruv" / "allt"

part = "allt";          // "kapa" | "skruv" | "allt"

/* [Kåpa] */
inner_d   = 120;        // innerdiameter vid taket (mm)
height    = 78;         // total höjd (mm). Djup innanför: ca 76 vid kanten av navet, 54 mitt i
wall      = 1.8;        // väggtjocklek (mm)
u_v       = 0.5;        // 0 = mer U (rakare sidor längre ner), 1 = mer V

/* [Kabel] */
cable_d   = 7;          // kabelns diameter (mm). Greppar ca cable_d-1.5 ... cable_d+1
bore      = cable_d + 1;

/* [Grov gänga] */
thr_d     = 26;         // ytterdiameter (mm)
pitch     = 8;          // stigning (mm) – väldigt grov
thr_h     = 2.4;        // gängdjup (mm), 45° flanker
thr_len   = 13;         // gänglängd (mm)
clear     = 0.4;        // spel (mm)

/* [Spännkona] */
cone_ang  = 25;
squeeze   = 1.2;
slots     = 4;
slot_w    = 1.4;

/* [Skruvskalle] */
head_d    = 40;
head_h    = 7;
lobes     = 8;

$fn = 96;

// ---------- härledda mått ----------
R         = inner_d/2;
hub_top   = 24;                         // navets topp (från bottenytan z=0)
hub_rt    = 8;                          // navets toppradie
r_floor   = hub_rt + (hub_top - wall);  // där navets 45°-kona möter golvet
k         = 2*tan(cone_ang);
z_cone0   = thr_len + 1;
d_cone0   = 16;
d_top     = bore + 0.6;
z_cone1   = z_cone0 + (d_cone0 - d_top)/k;

function cone_d(z) = d_cone0 - k*(z - z_cone0);

// ---------- profil: kubisk Bezier, alla tangenter >= 45° ----------
function bez(p0,p1,p2,p3,t) =
    pow(1-t,3)*p0 + 3*pow(1-t,2)*t*p1 + 3*(1-t)*t*t*p2 + pow(t,3)*p3;

d45   = 10 + 8*u_v;                       // längd på 45°-delen
P3    = [r_floor, wall];
P2    = [r_floor + d45, wall + d45];
P1    = [R, P2[1] + (R - P2[0]) + 6 + 20*u_v];
P0    = [R, height];
curve = [for (i = [0:60]) bez(P0, P1, P2, P3, i/60)];

module cavity2d() {
    polygon(concat([[0, height + 20], [R, height + 20]], curve,
                   [[hub_rt, hub_top], [0, hub_top]]));
}

module shell2d() {
    intersection() {
        difference() {
            union() {
                offset(r = wall, $fn = 32) cavity2d();
                polygon([[0,0], [r_floor + 1, 0], [r_floor + 1, wall], [hub_rt, hub_top], [0, hub_top]]);
            }
            cavity2d();
        }
        square([R + 10, height]);
    }
}

// ---------- grov trapetsgänga ----------
function prof(t) =
    let(c = (pitch - 2*thr_h)/2)
    t < c          ? 0 :
    t < c + thr_h  ? -(t - c) :
    t < 2*c + thr_h ? -thr_h :
                     -thr_h + (t - 2*c - thr_h);

module coarse_thread(d, len, extra = 0) {
    pts = [for (i = [0:2:358]) let(a = i, r = d/2 + prof(i/360*pitch))
           [r*cos(a), r*sin(a)]];
    linear_extrude(height = len, twist = -360*len/pitch,
                   slices = ceil(len/pitch*90), convexity = 8)
        offset(delta = extra) polygon(pts);
}

// ---------- kåpa ----------
module kapa() {
    difference() {
        rotate_extrude($fn = 240) shell2d();
        translate([0,0,-0.01]) coarse_thread(thr_d, thr_len + 0.02, clear);
        translate([0,0,thr_len - 0.01]) cylinder(d = thr_d - 2*thr_h + 2*clear, h = 1.02);
        translate([0,0,z_cone0]) cylinder(d1 = d_cone0, d2 = d_top, h = z_cone1 - z_cone0 + 0.01);
        cylinder(d = d_top, h = 100);
        translate([0,0,-0.01]) cylinder(d1 = thr_d + 1.5, d2 = thr_d - 2*thr_h, h = 2);
    }
}

// ---------- plastskruv (monterat läge: skallens översida i z=0) ----------
module skruv_assembled() {
    tip_z = z_cone1 - 0.6;
    difference() {
        union() {
            // skalle med greppflikar
            translate([0,0,-head_h]) {
                cylinder(d1 = head_d*0.72 - 2, d2 = head_d*0.72, h = 1);
                translate([0,0,1]) cylinder(d = head_d*0.72, h = head_h - 1);
                for (a = [0:360/lobes:359]) rotate(a)
                    translate([head_d/2 - 5, 0, 0]) hull() {
                        cylinder(d = 8, h = head_h - 1.5);
                        translate([0,0,head_h - 1.5]) cylinder(d = 5, h = 1.5);
                        translate([-6, 0, 0]) cylinder(d = 8, h = head_h);
                    }
            }
            // grov gänga
            intersection() {
                coarse_thread(thr_d, thr_len);
                cylinder(d1 = thr_d + 3, d2 = thr_d - 2*thr_h - 1, h = thr_len + 0.3);
            }
            // spännfingrar
            translate([0,0,thr_len - 0.01])
                cylinder(d = cone_d(z_cone0) + 2*squeeze - 0.4, h = z_cone0 - thr_len + 0.02);
            translate([0,0,z_cone0])
                cylinder(d1 = cone_d(z_cone0) + 2*squeeze - 0.4,
                         d2 = cone_d(tip_z) + 2*squeeze, h = tip_z - z_cone0);
        }
        translate([0,0,-head_h - 1]) cylinder(d = bore, h = 100);
        translate([0,0,-head_h - 0.01]) cylinder(d1 = bore + 4, d2 = bore, h = 2);
        for (a = [0:360/slots:359]) rotate(a + 45)
            translate([0, -slot_w/2, 1]) cube([thr_d, slot_w, 50]);
    }
}

module skruv() { translate([0,0,head_h]) skruv_assembled(); }

// ---------- utdata ----------
if (part == "kapa")  translate([0,0,height]) rotate([180,0,0]) kapa();   // utskriftsläge
if (part == "kapa_monterad") kapa();
if (part == "skruv") skruv();
if (part == "allt") {
    kapa();
    translate([R + head_d/2 + 12, 0, 0]) skruv();
}
