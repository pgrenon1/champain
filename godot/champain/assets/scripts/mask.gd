extends Node2D

@export var masked: Array[Sprite2D] = []

func _ready():
	var shader_code = """
	shader_type canvas_item;
	
	uniform sampler2D mask_texture;
	
	void fragment() {
		vec4 sprite = texture(TEXTURE, UV);
		vec4 mask = texture(mask_texture, UV);
		COLOR = sprite;
		COLOR.a *= mask.a;
	}
	"""
	
	var material = ShaderMaterial.new()
	material.shader = Shader.new()
	material.shader.code = shader_code
	material.set_shader_parameter("mask_texture", preload("res://assets/graphics/bottle/bottle_mask2.png"))
	
	for sprite in masked:
		sprite.material = material
