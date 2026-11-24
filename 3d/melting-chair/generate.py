"""
Melting monobloc chair – SDF model -> watertight mesh (STL + 3MF) for Bambu Studio.
Units: mm. Z up, front of chair = -Y, chair's right = +X. Puddle feet sit flat on Z=0.
Run:  pip install numpy scipy scikit-image trimesh fast-simplification lxml networkx
      python generate.py
"""
import numpy as np
from skimage.measure import marching_cubes
import trimesh, fast_simplification, os

RES = 0.34
TARGET_FACES = 500_000
OUT = os.path.dirname(os.path.abspath(__file__))

BMIN = np.array([-74.0, -72.0, 0.0])
BMAX = np.array([74.0, 68.0, 140.0])
xs = np.arange(BMIN[0], BMAX[0], RES, dtype=np.float32)
ys = np.arange(BMIN[1], BMAX[1], RES, dtype=np.float32)
zs = np.arange(BMIN[2] - 2, BMAX[2], RES, dtype=np.float32)
FAR = 20.0
field = np.full((len(xs), len(ys), len(zs)), FAR, dtype=np.float32)
MARGIN = 9.0
rng = np.random.default_rng(11)

# ------------------------------------------------------------------ SDF helpers
def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b + (a - b) * h - k * h * (1 - h)

def region(lo, hi):
    lo = np.asarray(lo) - MARGIN; hi = np.asarray(hi) + MARGIN
    i0 = [max(0, int(np.searchsorted(g, l))) for g, l in zip((xs, ys, zs), lo)]
    i1 = [int(np.searchsorted(g, h)) for g, h in zip((xs, ys, zs), hi)]
    sl = tuple(slice(a, b) for a, b in zip(i0, i1))
    X, Y, Z = np.meshgrid(xs[sl[0]], ys[sl[1]], zs[sl[2]], indexing="ij")
    return sl, X, Y, Z

def add(prim, k=2.0):
    fn, lo, hi = prim
    sl, X, Y, Z = region(lo, hi)
    if X.size: field[sl] = smin(field[sl], fn(X, Y, Z), k)

def sub(prim, k=1.0):
    fn, lo, hi = prim
    sl, X, Y, Z = region(lo, hi)
    if X.size: field[sl] = -smin(-field[sl], fn(X, Y, Z), k)

def cone_capsule(a, b, ra, rb, zsq=1.0):
    """capsule with linearly varying radius; zsq>1 flattens the cross-section vertically"""
    a = np.asarray(a, np.float32); b = np.asarray(b, np.float32)
    ba = (b - a) * [1, 1, zsq]; bb = float(ba @ ba) + 1e-9
    def f(X, Y, Z):
        px, py, pz = X - a[0], Y - a[1], (Z - a[2]) * zsq
        h = np.clip((px * ba[0] + py * ba[1] + pz * ba[2]) / bb, 0, 1)
        dx, dy, dz = px - ba[0] * h, py - ba[1] * h, pz - ba[2] * h
        return (np.sqrt(dx * dx + dy * dy + dz * dz) - (ra + (rb - ra) * h)) / zsq
    r = max(ra, rb)
    return f, np.minimum(a, b) - r, np.maximum(a, b) + r

def ellipsoid(c, r, sz=1.0):
    c = np.asarray(c, np.float32)
    def f(X, Y, Z):
        return (np.sqrt((X - c[0])**2 + (Y - c[1])**2 + ((Z - c[2]) * sz)**2) - r) / max(sz, 1.0)
    return f, c - r / min(sz, 1) , c + r / min(sz, 1)

def round_box(c, h, r):
    c = np.asarray(c, np.float32); h = np.asarray(h, np.float32)
    def f(X, Y, Z):
        qx = np.abs(X - c[0]) - h[0] + r; qy = np.abs(Y - c[1]) - h[1] + r; qz = np.abs(Z - c[2]) - h[2] + r
        out = np.sqrt(np.maximum(qx, 0)**2 + np.maximum(qy, 0)**2 + np.maximum(qz, 0)**2)
        return out + np.minimum(np.maximum(qx, np.maximum(qy, qz)), 0) - r
    return f, c - h, c + h

def catmull(pts, n=10):
    pts = [np.asarray(p, float) for p in pts]
    P = [pts[0]] + pts + [pts[-1]]
    out = []
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for t in np.linspace(0, 1, n, endpoint=False):
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-1])
    return np.array(out)

def tube(pts, radii, k=1.5, n=8, zsq=1.0):
    path = catmull(pts, n)
    rr = np.interp(np.linspace(0, 1, len(path)), np.linspace(0, 1, len(radii)), radii)
    for i in range(len(path) - 1):
        add(cone_capsule(path[i], path[i + 1], rr[i], rr[i + 1], zsq), k=k)
    return path

def drip(top, length, r, lean=(0.0, 0.0), k_top=2.5):
    """Viscous drip: wide fillet at the root, thin neck, heavy elongated bulb."""
    top = np.asarray(top, float); lean = np.array([lean[0], lean[1], 0.0])
    root = top + [0, 0, 1.5]
    neck = top + lean * 0.55 + [0, 0, -length * 0.62]
    end = top + lean + [0, 0, -length]
    p22 = top + lean * 0.2 + [0, 0, -length * 0.25]
    add(cone_capsule(root, p22, r * 1.9, r * 1.02), k=k_top)
    add(cone_capsule(p22, neck, r * 1.02, r * 0.95), k=2.5)
    add(cone_capsule(neck, end, r * 0.95, r * 1.0), k=2.5)
    add(ellipsoid(end + [0, 0, -r * 0.1], r * 1.0, sz=0.72), k=2.0)

def strand(a, b, r_end, r_mid):
    """stretched goo string connecting two surfaces (hourglass)"""
    a = np.asarray(a, float); b = np.asarray(b, float); m = (a + b) / 2
    add(cone_capsule(a, m, r_end, r_mid), k=2.5)
    add(cone_capsule(m, b, r_mid, r_end * 1.15), k=2.5)

# ------------------------------------------------------------------ one-piece seat+back shell (monobloc profile)
from scipy.ndimage import map_coordinates
SZ = 58.0
PROF = catmull([(-40.5, 58.0), (-20, 58.4), (0, 57.8), (14, 57.3), (22, 58.0), (27, 61.2), (29.4, 66.5),
                (31.6, 76), (35.5, 92), (40.5, 108), (45.0, 123), (47.0, 131), (48.0, 136)], n=14)
SEGL = np.linalg.norm(np.diff(PROF, axis=0), axis=1)
CUM = np.concatenate([[0], np.cumsum(SEGL)])
def s_at_z(z):   # arc length where the back reaches height z
    i = np.argmax((PROF[:, 1] >= z) & (PROF[:, 0] > 20)); return CUM[i]
def s_at_y(y):
    i = np.argmax(PROF[:, 0] >= y); return CUM[i]
S_TOP = s_at_z(127.0)
BACK_W = 40.0
def s_top(x):
    return S_TOP - 15.0 * (np.minimum(np.abs(x), BACK_W) / BACK_W) ** 2.0     # arched top edge
R_WRAP = 52.0
def wrapc(x):
    x = np.minimum(np.abs(x), R_WRAP - 1); return R_WRAP - np.sqrt(R_WRAP**2 - x * x)
def wrapw(z):
    t = np.clip((z - 60.0) / 22.0, 0, 1); return t * t * (3 - 2 * t)
def halfw(z):
    t = np.clip((z - 62.0) / 14.0, 0, 1); return 44.5 - 4.5 * t * t * (3 - 2 * t)

# 2-D distance / arc-length lookup of the profile
G = 0.17
GY = np.arange(-62, 78, G); GZ = np.arange(30, 146, G)
YY, ZZ = np.meshgrid(GY, GZ, indexing="ij")
D2 = np.full(YY.shape, 1e9); S2 = np.zeros(YY.shape)
for i in range(len(PROF) - 1):
    a_, b_ = PROF[i], PROF[i + 1]; ba = b_ - a_
    h = np.clip(((YY - a_[0]) * ba[0] + (ZZ - a_[1]) * ba[1]) / (ba @ ba), 0, 1)
    d = np.hypot(YY - a_[0] - ba[0] * h, ZZ - a_[1] - ba[1] * h)
    m = d < D2; D2[m] = d[m]; S2[m] = (CUM[i] + h * SEGL[i])[m]

def prof_lookup(X, Y, Z):
    ye = Y + wrapc(X) * wrapw(Z)
    c = np.stack([((ye - GY[0]) / G).ravel(), ((Z - GZ[0]) / G).ravel()])
    D = map_coordinates(D2, c, order=1, mode="nearest").reshape(X.shape)
    S = map_coordinates(S2, c, order=1, mode="nearest").reshape(X.shape)
    return D, S

def surf_pt(x, s, off=0.0):
    """point on the shell mid-surface at lateral x and arc length s (off>0 = towards the rear)"""
    y = np.interp(s, CUM, PROF[:, 0]); z = np.interp(s, CUM, PROF[:, 1])
    y2 = np.interp(s + 0.5, CUM, PROF[:, 0]); z2 = np.interp(s + 0.5, CUM, PROF[:, 1])
    t = np.array([y2 - y, z2 - z]); t /= np.linalg.norm(t); n = np.array([t[1], -t[0]])
    return np.array([x, y - wrapc(x) * wrapw(z) + off * n[0], z + off * n[1]])

SHELL_LO, SHELL_HI = (-46, -46, 50), (46, 58, 138)
def shell(X, Y, Z):
    D, S = prof_lookup(X, Y, Z)
    d = D - 3.0
    d = np.maximum(d, np.abs(X) - halfw(Z))
    return np.maximum(d, S - s_top(X))
add((shell, SHELL_LO, SHELL_HI), k=1.0)

def slot_xs(xa, xb, sa, sb, ra, rb):
    """tapered stadium cut in (x, arc-length) space, through the shell only"""
    def f(X, Y, Z):
        D, S = prof_lookup(X, Y, Z)
        dx, ds = xb - xa, sb - sa
        h = np.clip(((X - xa) * dx + (S - sa) * ds) / (dx * dx + ds * ds), 0, 1)
        d = np.hypot(X - xa - dx * h, S - sa - ds * h) - (ra + (rb - ra) * h)
        return np.maximum(d, D - 7.0)
    return f, SHELL_LO, SHELL_HI

# 4 long back slots, fanning out towards the top (left -> right)
SB = s_at_z(63.5)
for xt, st_off, sb_extra in ((-21.0, 7.5, 2.0), (-7.0, 7.0, 0.0), (7.0, 7.0, -2.0), (21.0, 7.5, 3.0)):
    sub(slot_xs(xt * 0.78, xt, SB + sb_extra, s_top(xt) - st_off, 3.0, 4.3), k=1.2)
# seat slots running front-to-back
for xc, y0, y1, w in ((-23, -18, 11, 2.6), (-8, -21, 13, 2.8), (8, -20, 13, 2.8), (23, -10, 10, 2.5)):
    sub(slot_xs(xc - 1.0, xc, s_at_y(y0), s_at_y(y1), w, w * 1.05), k=1.1)

# goo flowing from the back slats down over the seat (low, flat ridges)
for x in (-31, -15.5, 0, 15.5, 31):
    p0 = surf_pt(x * 0.85, SB + 4, off=-2.4)
    add(cone_capsule(p0, (x * 0.92, 18, SZ + 2.4), 2.6, 2.3, zsq=1.5), k=4.0)
    add(cone_capsule((x * 0.92, 18, SZ + 2.4), (x * 0.95, 2 + 6 * np.cos(x), SZ + 2.3), 2.3, 1.2, zsq=1.8), k=4.0)

# ------------------------------------------------------------------ seat edges
tube([(-45, -37.5, SZ - 2.6), (-20, -39.5, SZ - 3.0), (0, -39, SZ - 2.8), (20, -40, SZ - 3.4), (45, -37.5, SZ - 2.6)],
     [5.0, 5.4, 5.2, 5.6, 5.0], k=4.0)
add(round_box((0, -38.8, SZ - 6.0), (43, 3.4, 5.5), 3.0), k=3.0)
for x, dz in ((-30, 1.0), (-14, 2.0), (2, 0.8), (17, 2.4), (33, 1.4)):          # gentle sag waves
    add(ellipsoid((x, -38.8, SZ - 9.5 - dz), 8.0, sz=2.2), k=4.0)
for s in (-1, 1):
    tube([(s * 44.5, -36, SZ - 2.4), (s * 45.5, -6, SZ - 2.8), (s * 44, 22, SZ - 1.8)], [4.6, 4.8, 4.4], k=4.0)
    add(round_box((s * 45.2, -8, SZ - 5.0), (2.6, 25, 4.2), 2.2), k=3.0)
for c, r in (((20, -30, SZ + 1.6), 9.0), ((30, -33, SZ + 1.2), 7.0), ((-18, -31, SZ + 1.0), 7.5), ((4, -33, SZ + 1.0), 6.5)):
    add(ellipsoid(c, r, sz=3.2), k=4.0)

# ------------------------------------------------------------------ rim + back side edges
tube([surf_pt(x, s_top(x) - 0.8) for x in np.linspace(-BACK_W, BACK_W, 13)],
     [3.9, 4.0, 4.1, 4.2, 4.2, 4.2, 4.2, 4.2, 4.2, 4.1, 4.2, 4.4, 4.7], k=2.2)
for s in (-1, 1):
    ss = np.linspace(s_at_z(64), s_top(BACK_W) - 2, 5)
    tube([surf_pt(s * (halfw(np.interp(v, CUM, PROF[:, 1])) - 0.5), v) for v in ss], [3.4, 3.0, 2.9, 3.1, 3.5], k=2.5)

# ------------------------------------------------------------------ ARMS: roll off the rim, slope down, post into seat
ARM = {}
for s in (-1, 1):
    c0 = surf_pt(s * BACK_W, s_top(BACK_W) - 1.0)
    pts = [c0, c0 + [s * 6.5, -3.0, -7.0], c0 + [s * 11.0, -9, -18], (s * 53.5, 6, 87.5),
           (s * 53.5, -5, 83), (s * 52.5, -18, 80), (s * 50.8, -29, 78.5), (s * 49.5, -36.5, 75),
           (s * 48, -39.5, 68.5), (s * 47, -39, 62), (s * 46, -37, 56)]
    ARM[s] = tube(pts, [4.1, 4.1, 4.0, 4.0, 4.1, 4.2, 4.3, 4.5, 4.8, 5.2, 5.8], k=2.6, n=10, zsq=1.25)
    add(ellipsoid(c0 + [s * 2.0, -1.0, -2.0], 4.4, sz=1.1), k=4.0)
C0R = surf_pt(BACK_W, s_top(BACK_W) - 1.0)
C0L = surf_pt(-BACK_W, s_top(BACK_W) - 1.0)
print("back top corner:", np.round(C0R, 1), " top centre:", np.round(surf_pt(0, S_TOP), 1))

def arm_at(s, y, below=3.5, out=0.0):
    p = ARM[s][20:]
    q = p[np.argmin(np.abs(p[:, 1] - y))].copy()
    return q + [s * out, 0, -below]

# right shoulder has slumped: sagging mass hanging into the arm opening
for off, r, sz in (([4.5, -2, -5], 4.8, 1.0), ([5.5, -3.5, -10], 3.8, 0.9)):
    add(ellipsoid(C0R + off, r, sz), k=3.5)
drip(C0R + [5.5, -3.5, -12], 13, 2.7, k_top=3.0)

# ------------------------------------------------------------------ LEGS + PUDDLES
LEGS = {(-1, "f"): ((-44, -33, 55), (-49.5, -45, 4)), (1, "f"): ((44, -33, 55), (49.5, -45, 4)),
        (-1, "b"): ((-42, 22, 55), (-50, 42, 4)), (1, "b"): ((42, 22, 55), (50, 42, 4))}
for (s, fb), (a, b) in LEGS.items():
    a = np.array(a, float); b = np.array(b, float)
    add(cone_capsule(a, b, 7.6, 5.0), k=7.0)
    # big lobed puddle with a thick rounded rim
    cx, cy = b[0] + s * 1.5, b[1] + (-2 if fb == "f" else 2)
    add(ellipsoid((cx, cy, 0.8), 11.0, sz=2.4), k=6.0)
    for j in range(6):
        ang = j * np.pi / 3 + rng.uniform(-0.4, 0.4); dist = rng.uniform(7.0, 11.0)
        add(ellipsoid((cx + dist * np.cos(ang), cy + dist * np.sin(ang), 0.6), rng.uniform(5.0, 7.5), sz=2.0), k=3.0)

# ------------------------------------------------------------------ DRIPS
# front edge: few but heavy drips, one very long (right of centre)
for x, L, r, ln in ((-9, 7, 3.4, -0.3), (5, 13, 3.4, -0.5), (12.5, 31, 3.9, -1.2), (23, 18, 3.5, -0.7), (31.5, 12, 3.1, -0.4)):
    drip((x, -41.3, SZ - 10.0), L + 1.5, r, lean=(0, ln))
# front-left corner: seat goo flowing over onto the front-left leg
add(cone_capsule((-38, -41, SZ - 8), (-46, -44.5, 30), 4.0, 2.8), k=3.0)
add(cone_capsule((-46, -44.5, 30), (-49.5, -47.5, 8), 2.8, 3.2), k=3.0)
add(cone_capsule((40, -41, SZ - 8), (46.5, -44.5, 36), 3.6, 2.6), k=3.0)
# right arm: drips hanging under the arm into the opening + a goo string to the seat
for y, L, r in ((17, 14, 2.5), (7, 16, 2.6), (-14, 19, 2.7)):
    drip(arm_at(1, y, below=3.2, out=-2.0), L, r, k_top=2.8)
strand(arm_at(1, -3, below=2.0, out=-1.5), (46.0, -4, SZ + 2.5), 3.4, 2.1)
# left arm: small drip inside the shoulder loop, two on the outside
drip(C0L + [-4.5, -3.0, -6.0], 10, 2.3, k_top=2.8)
for y, L, r in ((12, 14, 2.5), (-2, 25, 2.9)):
    drip(arm_at(-1, y, below=3.0, out=2.0), L, r, lean=(-0.6, 0), k_top=2.8)
# seat sides
drip((-47.5, -16, SZ - 8.5), 9, 2.4, lean=(-0.5, 0))
drip((47.5, 8, SZ - 8.5), 7, 2.2, lean=(0.5, 0))
# back side of the rim
for x, L, r in ((-20, 7, 2.3), (14, 10, 2.4), (30, 13, 2.6)):
    drip(surf_pt(x, s_top(x) - 3.0, off=4.0), L, r, lean=(0, 0.8))

# ------------------------------------------------------------------ mesh
X, Y, Z = np.meshgrid(xs, ys, zs, indexing="ij", sparse=True)
field += (0.18 * np.sin(0.23 * X + 1.3) * np.sin(0.19 * Y + 0.4) * np.sin(0.21 * Z + 0.5)).astype(np.float32)
field = np.maximum(field, -Z.astype(np.float32))      # flat bottom at Z=0
field[0], field[-1] = FAR, FAR
field[:, 0], field[:, -1] = FAR, FAR
field[:, :, 0], field[:, :, -1] = FAR, FAR

verts, faces, _, _ = marching_cubes(field, level=0.0, spacing=(RES, RES, RES))
verts += np.array([xs[0], ys[0], zs[0]])
v2, f2 = fast_simplification.simplify(verts.astype(np.float32), faces.astype(np.int64),
                                      target_reduction=1 - TARGET_FACES / len(faces))
mesh = trimesh.Trimesh(v2, f2, process=True)
mesh.update_faces(mesh.nondegenerate_faces()); mesh.remove_unreferenced_vertices()
parts = mesh.split(only_watertight=False)
mesh = max(parts, key=lambda m: len(m.faces))
trimesh.repair.fix_normals(mesh)
mesh.apply_translation([0, 0, -mesh.bounds[0][2]])
print("faces:", len(mesh.faces), "watertight:", mesh.is_watertight, "winding ok:", mesh.is_winding_consistent,
      "volume cm3: %.1f" % (mesh.volume / 1000), "extents:", np.round(mesh.extents, 1), "parts:", len(parts))
mesh.export(os.path.join(OUT, "melting_chair.stl"))
mesh.export(os.path.join(OUT, "melting_chair.3mf"))
