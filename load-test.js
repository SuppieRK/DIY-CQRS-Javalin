// See https://grafana.com/docs/k6/latest/set-up/install-k6/
// See https://grafana.com/docs/k6/latest/misc/integrations/
import http from 'k6/http';
import {check} from 'k6';

export const options = {
    // Key configurations for Stress test in this section
    stages: [
        {duration: '1m', target: 100}, // Traffic ramp-up from 1 to X users over 1 minute.
        {duration: '8m', target: 100}, // Stay at X users for 8 minutes.
        {duration: '1m', target: 0}, // Ramp-down to 0 users over 1 minute.
    ],
    summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'p(99.99)', 'max', 'count'],
};

export default function () {
    const defaultUser = JSON.stringify({
        username: 'username', password: 'password', email: 'email'
    });

    const userUpdatePayload = JSON.stringify({
        username: 'newUsername'
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    let usersUrl = http.url`http://localhost:8080/users`;
    let userId = ``;
    let userUrl;

    let createdUserResponse = http.post(usersUrl, defaultUser, params);

    if (check(createdUserResponse, {'is status 200': (r) => r.status === 200})) {
        userId = createdUserResponse.json('id');
        userUrl = http.url`http://localhost:8080/users/${userId}`;
    } else {
        console.log(`Unable to create a user ${createdUserResponse.status} ${createdUserResponse.body}`);
        return;
    }

    let getAllUsersResponse = http.get(usersUrl, params);
    check(getAllUsersResponse, {
        'is status 200': (r) => r.status === 200,
        'is contains user with id': (r) => r.json().some(u => u.id === userId),
    });

    let getUserByIdResponse = http.get(userUrl, params);
    check(getUserByIdResponse, {
        'is status 200': (r) => r.status === 200,
        'is user with id': (r) => r.json().id === userId,
    });

    let updatedUserResponse = http.put(userUrl, userUpdatePayload, params);
    const isSuccessfulUpdate = check(updatedUserResponse, {
        'is status 200': (r) => r.status === 200,
        'is user with updated username': (r) => r.json().username === 'newUsername',
    });

    if (!isSuccessfulUpdate) {
        console.log(`Unable to update the user ${updatedUserResponse.status} ${updatedUserResponse.body}`);
        return;
    }

    const deleteUserResponse = http.del(userUrl, null, params);

    const isSuccessfulDelete = check(null, {
        'user was deleted correctly': () => deleteUserResponse.status === 200,
    });

    if (!isSuccessfulDelete) {
        console.log(`User was not deleted properly`);
        return;
    }

    let getAllUsersResponseAfterDeletion = http.get(usersUrl, params);
    check(getAllUsersResponseAfterDeletion, {
        'is status 200': (r) => r.status === 200,
        'does not contain user with id': (r) => !r.json().some(u => u.id === userId),
    });
};