class_name Player extends RigidBody2D

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
	pass
	
func _physics_process(delta: float) -> void:
	var player_pointing_angle = ControllerManager.instance.get_player_angle(player_id)
	$debug.global_rotation = player_pointing_angle

	var direction = Vector2.ZERO
	
	var boost = false
	
	if use_mouse:
		direction = position - get_viewport().get_mouse_position()
		direction = direction.normalized()
		
		if !_is_spraying and Input.is_action_just_pressed("shake" + str(player_id+1))\
		and _shake_count < spray_shake_max_count:
			_shake_count += 1
			print("P" + str(player_id) + ": " + str(_shake_count) + "shakes")
			
		if !_is_spraying and Input.is_action_just_pressed("move" + str(player_id+1)):
			_start_spraying(direction)
	else:
		direction = -Vector2.from_angle(player_pointing_angle)
		
		if !_is_spraying and ControllerManager.instance.get_shake_down(player_id):
			if _shake_count < spray_shake_max_count:
				_shake_count += 1
				print("P" + str(player_id) + ": " + str(_shake_count) + "shakes")
			
		if !_is_spraying and false: #& controller pop
			_start_spraying(direction)
	
	if _is_spraying:
		_spray_timer -= delta
		apply_force(direction * _spray_force)
		if _spray_timer <= 0:
			_is_spraying = false

func _start_spraying(direction: Vector2):
	_is_spraying = true
	var pop_power = _shake_count/float(spray_shake_max_count)
	print("P" + str(player_id) + " pop power: " + str(pop_power) + str(spray_max_duration))
	var impulse = lerp(spray_min_impulse, spray_max_impulse, pop_power)
	_spray_force = lerp(spray_min_force, spray_max_force, pop_power)
	_spray_timer = lerp(spray_min_duration, spray_max_duration, pop_power)
	apply_impulse(direction * impulse)
	_shake_count = 0

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
