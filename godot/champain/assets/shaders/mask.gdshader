	shader_type canvas_item;

	uniform sampler2D mask_texture;

	void fragment() {
		vec4 sprite = texture(TEXTURE, UV);
		vec4 mask = texture(mask_texture, UV);
		COLOR = sprite;
		COLOR.a *= mask.a;
	}