extends Sprite2D

@export var launch_speed = 0.0

var _initial_position = Vector2.ZERO
var _launched_duplicate: Sprite2D = null

@export_category("Debug")
@export var launch_button: bool:
	set(value):
		launch()
		
@export_category("Debug")
@export var reset_button: bool:
	set(value):
		reset()

func launch():
	if _launched_duplicate != null:
		return
		
	# Create new Sprite2D and copy properties
	_launched_duplicate = Sprite2D.new()
	_launched_duplicate.texture = texture
	_launched_duplicate.scale = scale
	_launched_duplicate.offset = offset
	_launched_duplicate.flip_h = flip_h
	_launched_duplicate.flip_v = flip_v
	
	# Set up movement script
	_launched_duplicate.set_script(preload("res://assets/scripts/projectile.gd"))
	get_tree().root.add_child(_launched_duplicate)
	_launched_duplicate.global_transform = global_transform
	
	# Setup duplicate's movement
	_launched_duplicate.launch_speed = launch_speed
	_launched_duplicate.launch()
	
	visible = false
		
func _ready():
	_initial_position = global_position

func reset():
	if _launched_duplicate:
		_launched_duplicate.queue_free()
		_launched_duplicate = null
	visible = true
