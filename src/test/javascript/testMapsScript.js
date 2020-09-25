describe('Testing mapsScript.js', () => {

  it('creates the correct description for the waypoints', () => {
    const waypoints = [
      {
        'point': {'latitude':0.0, 'longitude':0.0},
        'orderKeys': []
      },
      {
        'point': {'latitude':1.0, 'longitude':1.0, 'libraryId':1},
        'orderKeys': ['id1', 'id2']
      },
      {
        'point': {'latitude':2.0, 'longitude':2.0},
        'orderKeys': ['id1', 'id2']
      }
    ];
    expect(getWaypointDescription(0, waypoints)).toEqual('Start Here');
    expect(getWaypointDescription(1, waypoints)).toEqual('Borrow books for orders:\nid1\nid2');
    expect(getWaypointDescription(2, waypoints)).toEqual('Deliver orders:\nid1\nid2');
    })

    it('creates the correct description for the waypoints when start point is also a library', () => {
    const waypoints = [
      {
        'point': {'latitude':1.0, 'longitude':1.0, 'libraryId':1},
        'orderKeys': ['id1', 'id2']
      },
      {
        'point': {'latitude':2.0, 'longitude':2.0},
        'orderKeys': ['id1', 'id2']
      }
    ];
    expect(getWaypointDescription(0, waypoints)).toEqual('Borrow books for orders:\nid1\nid2');
    expect(getWaypointDescription(1, waypoints)).toEqual('Deliver orders:\nid1\nid2');
    })

    it('centers the map correctly', () => {
    const waypoints = [
      {
        'point': {'latitude':0.0, 'longitude':0.0},
        'orderKeys': []
      },
      {
        'point': {'latitude':1.0, 'longitude':1.0, 'libraryId':1},
        'orderKeys': ['id1', 'id2']
      },
      {
        'point': {'latitude':2.0, 'longitude':2.0},
        'orderKeys': ['id1', 'id2']
      }
    ];
    expect(getMapCenter(waypoints)).toEqual({'latitude': 1.0, 'longitude': 1.0});
    })
});
