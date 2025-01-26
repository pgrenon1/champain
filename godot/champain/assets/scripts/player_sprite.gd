@tool
extends Sprite2D

@export var float_amplitude = 5.0
@export var sprites : Array[Texture2D] = []

var _original_position


func set_sprite():
	self.texture = sprites[self.get_parent().player_id]
# Called when the node enters the scene tree for the first time.
func _ready():
	self.texture = sprites[self.get_parent().player_id]
	
	_original_position = position
	position += Vector2.DOWN * (float_amplitude/2)
	var tween = get_tree().create_tween()
	tween.tween_property(self, "position", _original_position + Vector2.UP * float_amplitude, 1.5+randf())\
	.set_trans(Tween.TRANS_SINE)
	tween.tween_property(self, "position", _original_position + Vector2.DOWN * float_amplitude, 1.5+randf())\
	.set_trans(Tween.TRANS_SINE)
	tween.set_loops()

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	if Engine.is_editor_hint():
		set_sprite()
