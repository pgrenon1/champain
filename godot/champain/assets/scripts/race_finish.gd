class_name RaceFinish extends Area2D

static var instance

@export var number_of_laps = 1
var _checkpoint_count = 0
var player_laps = {} # Dict to store laps for each player

var is_over = false

func _ready() -> void:
	RaceFinish.instance = self 
	body_entered.connect(_entered)

func _process(delta: float) -> void:
	pass
	
func add_checkpoint():
	_checkpoint_count += 1
	#print("Checkpoint " + str(_checkpoint_count) + " added")
	return _checkpoint_count
		
func _entered(body):
	if not is_over:
		if body is Player:
			if body.validated_checkpoints.size() >= _checkpoint_count:
				body.validate_lap()
				
				# Initialize player laps if not already in dict
				if not player_laps.has(body):
					player_laps[body] = 0
					
				# Increment laps for this player
				player_laps[body] += 1
				print("Player completed lap " + str(player_laps[body]))
				
				# Check for winner
				if player_laps[body] >= number_of_laps:
					%Announcer.visible = true
					%Announcer.text = "Player %s WINS!" % [str(body.player_id+1)]
					is_over = true
					await get_tree().create_timer(5.0).timeout
					Global.goto_scene("res://assets/scenes/lobby.tscn")
