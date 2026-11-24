"""Packar spiral_galler_170mm.stl till en 3MF för Bambu Studio.

Objektet centreras på plattan (256x256) och får inbäddade objektinställningar
(0,2 mm lager, 4 väggar, 100 % infill). Välj PETG-filament i Bambu Studio.
Kör:  python3 make_3mf.py   -> spiral_galler_170mm.3mf
"""
import zipfile
import trimesh

STL, OUT, NAME = "spiral_galler_170mm.stl", "spiral_galler_170mm.3mf", "Spiralgaller 170mm"
PLATE_CENTER = (128.0, 128.0)
SETTINGS = {
    "layer_height": "0.2",
    "wall_loops": "4",
    "sparse_infill_density": "100%",
    "top_shell_layers": "5",
    "bottom_shell_layers": "4",
}

m = trimesh.load(STL)
m.merge_vertices()
m.apply_translation([-m.bounds[:, 0].mean(), -m.bounds[:, 1].mean(), -m.bounds[0, 2]])

verts = "\n".join(f'<vertex x="{x:.4f}" y="{y:.4f}" z="{z:.4f}"/>' for x, y, z in m.vertices)
tris = "\n".join(f'<triangle v1="{a}" v2="{b}" v3="{c}"/>' for a, b, c in m.faces)
cx, cy = PLATE_CENTER
model = f'''<?xml version="1.0" encoding="UTF-8"?>
<model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02">
<metadata name="Title">{NAME}</metadata>
<resources>
<object id="1" name="{NAME}" type="model">
<mesh>
<vertices>
{verts}
</vertices>
<triangles>
{tris}
</triangles>
</mesh>
</object>
</resources>
<build>
<item objectid="1" transform="1 0 0 0 1 0 0 0 1 {cx} {cy} 0" printable="1"/>
</build>
</model>
'''
meta = "\n".join(f'    <metadata key="{k}" value="{v}"/>' for k, v in SETTINGS.items())
model_settings = f'''<?xml version="1.0" encoding="UTF-8"?>
<config>
  <object id="1">
    <metadata key="name" value="{NAME}"/>
    <metadata key="extruder" value="1"/>
{meta}
    <part id="1" subtype="normal_part">
      <metadata key="name" value="{NAME}"/>
      <metadata key="matrix" value="1 0 0 0 0 1 0 0 0 0 1 0 0 0 0 1"/>
      <metadata key="source_object_id" value="0"/>
      <metadata key="source_volume_id" value="0"/>
    </part>
  </object>
  <plate>
    <metadata key="plater_id" value="1"/>
    <metadata key="plater_name" value=""/>
    <metadata key="locked" value="false"/>
    <model_instance>
      <metadata key="object_id" value="1"/>
      <metadata key="instance_id" value="0"/>
      <metadata key="identify_id" value="1"/>
    </model_instance>
  </plate>
</config>
'''
content_types = '''<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>
<Default Extension="png" ContentType="image/png"/>
</Types>
'''
rels = '''<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Target="/3D/3dmodel.model" Id="rel-1" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
</Relationships>
'''
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", rels)
    z.writestr("3D/3dmodel.model", model)
    z.writestr("Metadata/model_settings.config", model_settings)
print("skrev", OUT, len(m.vertices), "hörn", len(m.faces), "trianglar")
