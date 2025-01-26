@tool

extends Node2D

@export var max_shake_amount: float = 5.0  # Maximum shake offset
@export var shake_speed: float = 20.0  # Speed of oscillation
@export var is_shaking: bool = false
@export var curve: Curve

@export var scale_fix = 1.0


var shake_amount = 0.0
var initial_position: Vector2
var time: float = 0.0

func _ready():
	if 'player_id' in get_parent().get_parent():
		var mask_layer = get_parent().get_parent().player_id + 1
		$mask.range_item_cull_mask = (1 << mask_layer)
		$particles.light_mask = (1 << mask_layer)
	initial_position = position

func _process(delta):
	if Engine.is_editor_hint():
		self.scale = Vector2(scale_fix, scale_fix)
	else:
		if is_shaking:
			time += delta * shake_speed
			
			# Create shake offset using sin wave
			var offset_x = sin(time * 2.0) * shake_amount
			var offset_y = sin(time * 2.5) * shake_amount  # Slightly different frequency for y
			
			# Apply shake
			position = initial_position + Vector2(offset_x, offset_y)
		else:
			position = initial_position
			time = 0.0

# Call this to start shaking
func start_shake():
	is_shaking = true

# Call this to stop shaking
func stop_shake():
	is_shaking = false


func set_shake_value(value):
	self.scale.x = 1 + (0.5*value)
	$particles.amount_ratio = value
	$particles.process_material.scale.x = (2 + 1.5 * value) * scale_fix
	$particles.process_material.emission_box_extents = Vector3(1 + value, 1+ value, 1 + value)
	self.shake_amount = curve.sample(value) * max_shake_amount


func _on_wds_value_changed(value):
	set_shake_value(value)
