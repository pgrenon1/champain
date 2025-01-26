@tool
extends Sprite2D

@export var sprites : Array[Texture2D] = []



func set_sprite():
	self.texture = sprites[self.get_parent().player_id]
# Called when the node enters the scene tree for the first time.
func _ready():
	self.texture = sprites[self.get_parent().player_id]

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	if Engine.is_editor_hint():
		set_sprite()
