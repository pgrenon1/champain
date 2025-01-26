class_name Player extends RigidBody2D

@export var debug = false
@export var speed = 400
@export var spray_shake_max_count = 20
@export var spray_min_impulse = 0
@export var spray_max_impulse = 700
@export var spray_min_force = 0
@export var spray_max_force = 100
@export var spray_min_duration = 0.0
@export var spray_max_duration = 3.0

@export var player_id = 0
@export var _sprite: Sprite2D
@export var _label: Label
var _original_scale
@export var use_mouse = false
var _spray_timer = 0.0
var _spray_force
var _shake_count = 0
var _is_spraying = false
var lap_count = 0
var validated_checkpoints = []

var player_controller

func _ready() -> void:
	_original_scale = _sprite.scale
	_label.text = str(_shake_count)
	
	
func _process(delta: float) -> void:
	$debug.visible = self.debug
	
func _physics_process(delta: float) -> void:
	if not (ControllerManager.instance != null):
		return

	var player_pointing_angle
	if use_mouse:
		var mouse_pos = get_viewport().get_mouse_position()
		player_pointing_angle = (mouse_pos - position).angle()
	else:
		player_pointing_angle = ControllerManager.instance.get_player_angle(player_id)
	
	$debug.global_rotation = player_pointing_angle
	$bottle.global_rotation = player_pointing_angle + PI / 2

	var direction = Vector2.ZERO
	
	var boost = false
	
	if use_mouse:
		direction = position - get_viewport().get_mouse_position()
		direction = direction.normalized()
		
		if !_is_spraying and Input.is_action_just_pressed("shake" + str(player_id+1)):
			_shake()
			
		if !_is_spraying and Input.is_action_just_pressed("move" + str(player_id+1)):
			_start_spraying(direction)
	else:
		direction = -Vector2.from_angle(player_pointing_angle)
		
		if !_is_spraying and ControllerManager.instance.get_shake_down(player_id):
			_shake()
			
		if !_is_spraying and ControllerManager.instance.get_pop_down(player_id):
			_start_spraying(direction)
	
	if _is_spraying:
		_spray_timer -= delta
		apply_force(direction * _spray_force)
		if _spray_timer <= 0:
			_is_spraying = false
			_label.text = str(_shake_count)

func _shake():
	if _shake_count < spray_shake_max_count:
		_shake_count += 1
		_label.text = str(_shake_count)
	else:
		_label.text = "MAX"
		
	sync_bottle_shake()

func sync_bottle_shake():
	$bottle/bottle.set_shake_value(float(_shake_count) / float(spray_shake_max_count))

func _start_spraying(direction: Vector2):
	_is_spraying = true
	var pop_power = _shake_count/float(spray_shake_max_count)
	_label.text = "POP!"
	var impulse = lerp(spray_min_impulse, spray_max_impulse, pop_power)
	_spray_force = lerp(spray_min_force, spray_max_force, pop_power)
	_spray_timer = lerp(spray_min_duration, spray_max_duration, pop_power)
	apply_impulse(direction * impulse)
	_shake_count = 0
	sync_bottle_shake()

func validate_checkpoint(checkpoint_id: int):
	if !validated_checkpoints.has(checkpoint_id):
		validated_checkpoints.append(checkpoint_id)
		#print("Player " + str(player_id) + " got checkpoint " + str(checkpoint_id))
		
func validate_lap():
	lap_count += 1
	validated_checkpoints.clear()
	_sprite.scale = _original_scale * 2
	var tween = get_tree().create_tween()
	tween.tween_property(_sprite, "scale", _original_scale, 0.2)
	print("player " + str(player_id) + " finishes lap " + str(lap_count))
