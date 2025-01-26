class_name RaceFinish extends Area2D

static var instance

@export var number_of_laps = 3
var _checkpoint_count = 0

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
	if body is Player:
		if body.validated_checkpoints.size() >= _checkpoint_count:
			body.lap_count += 1
			body.validated_checkpoints.clear()
			print("player " + str(body.player_id) + " finishes lap " + str(body.lap_count))
