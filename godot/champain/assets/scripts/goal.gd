extends Area2D

@export var team_id = 0
# Called when the node enters the scene tree for the first time.
func _ready() -> void:
	body_entered.connect(ball_entered)


# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta: float) -> void:
	pass
	
func ball_entered(body):
	if !body is Ball:
		return
	SoccerGamemode.instance.goal_scored(team_id)
