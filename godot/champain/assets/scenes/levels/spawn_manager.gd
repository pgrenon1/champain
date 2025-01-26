extends Node

@export var player_count = 2
@export var player_scene: PackedScene
var _spawn_positions = []
# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	_get_spawn_positions()
	for i in player_count:
		var player = player_scene.instantiate()
		player.position = _spawn_positions[i]
		add_child(player)
	pass # Replace with function body.


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	pass

func _get_spawn_positions():
	var _spawn_parent = get_parent().find_child("SpawnPositions")
	if !_spawn_parent:
		push_error("Scene has no object named SpawnPositions")
		return
	var _children = _spawn_parent.find_children("Spawn*")
	for child in _children:
		_spawn_positions.append(child.position)
		
