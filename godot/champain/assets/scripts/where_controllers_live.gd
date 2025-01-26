class_name ControllerManager
extends Node

static var instance: ControllerManager = null

var players = {}  # Dictionary for player tracking
var players_angles = {}  # Dictionary for angles
var player_ids = []  # List to maintain player IDs in order

# Input state tracking
var _current_frame_shakes = {}  # Shakes that happened this frame
var _last_frame_shakes = {}    # Shakes that happened last frame
var _shake_timestamps = {}     # Last shake timestamp per player

var _current_frame_pops = {}    # Pops that happened this frame
var _last_frame_pops = {}      # Pops that happened last frame
var _pop_timestamps = {}       # Last pop timestamp per player

func _ready():
	self.instance = self
	
func clear_logic():
	# await end of frame to clear logic
	await get_tree().process_frame
	_last_frame_shakes = _current_frame_shakes.duplicate()
	_current_frame_shakes.clear()
	_last_frame_pops = _current_frame_pops.duplicate()
	_current_frame_pops.clear()

func _process(_delta):
	self.clear_logic.call_deferred()

static func get_pop_down(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._current_frame_pops

static func get_pop_up(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._last_frame_pops and not player_id in instance._current_frame_pops

static func get_pop(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._last_frame_pops

# Input interface methods
static func get_shake_down(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._current_frame_shakes

static func get_shake_up(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._last_frame_shakes and not player_id in instance._current_frame_shakes

static func get_shake(player_num: int) -> bool:
	if not is_instance_valid(instance):
		return false
	
	if player_num >= instance.player_ids.size():
		return false
		
	var player_id = instance.player_ids[player_num]
	return player_id in instance._last_frame_shakes

static func get_angle(player_num: int) -> float:
	if not is_instance_valid(instance):
		return 0.0
	return instance.get_player_angle(player_num)

func _on_quaternion(player_id, quat):
	if not player_id in players:
		players[player_id] = true
		players_angles[player_id] = 0.0
		if not player_id in player_ids:
			player_ids.append(player_id)
	
	var rotated_UP = quat * Vector3.UP
	var normal = Vector3(0, 0, 1)
	var projected = rotated_UP - rotated_UP.project(normal)
	projected = Vector2(projected.x, projected.y)
	
	players_angles[player_id] = -projected.angle()

func get_player_angle(player_num: int) -> float:
	if player_num < 0:
		return 0.0
	
	if player_num >= player_ids.size():
		return 0.0
		
	var player_id = player_ids[player_num]
	return players_angles.get(player_id, 0.0)

func _on_shake(player_id, timestamp):
	_current_frame_shakes[player_id] = true
	_shake_timestamps[player_id] = timestamp
	
func _on_pop(player_id, timestamp):
	_current_frame_pops[player_id] = true
	_pop_timestamps[player_id] = timestamp

func _on_node_2d_message_received(address, value, time):
	if address == '/quaternion':
		var player_id = value[0]
		var quaternion = Quaternion(value[1], value[2], value[3], value[4])
		_on_quaternion(player_id, quaternion)
		
	if address == '/shake':
		var player_id = value[0]
		var timestamp = value[1].to_int()
		_on_shake(player_id, timestamp)
		
	if address == '/pop':
		# HEY, value is a *string* here, so we don't do `value[0]`, we do `value`
		var player_id = value
		_on_pop(player_id, time)
