extends Node

@export var player_count = 2
@export var player_scene: PackedScene

static var instance: SpawnManager = null

var _spawn_positions : Array[Vector2] = []

var spawn_positions : Array[Vector2]:
	get:
		_get_spawn_positions()
		return _spawn_positions
		
var _spawned_players = 0

var players: Array[Player] = []

# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	if instance == null:
		instance = self
		
	_get_spawn_positions()
	ControllerManager.instance.player_added.connect(player_added)
	Global.scene_loaded.connect(on_scene_loaded)
	
func on_scene_loaded():
	spawn_players()
		
func spawn_players():
	players = []
	for i in range(_spawned_players):
		var player: Player = load("res://assets/scenes/player.tscn").instantiate()
		player.player_id = i
		spawn_player(player, spawn_positions[i])
		
		
func respawn_all():
	for p in players:
		p.queue_free()
		
	players = []
	spawn_players.call_deferred()
	
func player_added(player_id: int):
	var player: Player = load("res://assets/scenes/player.tscn").instantiate()
	player.player_id = player_id
	spawn_player.call_deferred(player, spawn_positions[_spawned_players])
	_spawned_players += 1
	
func spawn_player(player, spawn_position):
	get_tree().current_scene.add_child(player)
	player.global_position = spawn_position
	
	players.append(player)
	
# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	pass

func _get_spawn_positions():
	var spawn_parents = get_tree().get_nodes_in_group("spawn_positions")
	if len(spawn_parents) > 0:
		var _spawn_parent = spawn_parents[0]
		if !_spawn_parent:
			push_error("Scene has no object named spawn_positions")
			return
		var _children = _spawn_parent.find_children("Spawn*")
		self._spawn_positions = []
		for child in _children:
			self._spawn_positions.append(child.global_position)
		
