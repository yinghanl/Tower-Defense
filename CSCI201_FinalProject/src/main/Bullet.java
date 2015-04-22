package main;

public class Bullet extends Moveable{
	int direction;
	int damage;
	
	public Bullet(Space loc, int direction, int val){
		super(loc);
		System.out.println(loc);
		this.direction = direction;
		damage = val;
	}
	
	public void run(){
		while(true){
			try {
				move(direction);
				//if there is a creep in that location, hit it
				if(currentLocation instanceof PathSpace){
					PathSpace space = (PathSpace)currentLocation;
					if(space.isOccupied()){
						space.getCreep().hit(damage);
					}
				}
				sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BoundaryException be){
				break;
			}
			
		}
	}
	
	

}
